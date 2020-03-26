package com.e4d.job

import com.e4d.docker.DockerContainerShell
import com.e4d.docker.DockerTool
import com.e4d.dotnet.DotnetTool
import com.e4d.file.FindTool
import com.e4d.git.GitConfig
import com.e4d.git.GitSourceReference
import com.e4d.nexus.NexusClient
import com.e4d.nexus.NexusConfig
import com.e4d.nexus.NexusNugetRepository
import com.e4d.nuget.NugetConfig
import com.e4d.nuget.NugetRepository
import com.e4d.pipeline.PipelineShell
import com.e4d.pipeline.PipelineTextValueResolver
import com.e4d.step.CheckoutRecentSourceStep
import java.net.URI

class IntegrateNugetPackageJob extends PipelineJob {
  def packet
  def source
  DockerTool docker
  FindTool fileFinder
  GitConfig gitConfig = new GitConfig()
  GitSourceReference gitSourceRef = new GitSourceReference()
  NexusConfig nexusConfig = new NexusConfig()
  NugetConfig nugetConfig = new NugetConfig()
  String dockerfile
  String image
  URI dockerRegistry

  final projectFilePattern = /.+\.csproj/
  final testProjectFilePattern = /.+\.[Tt]ests?\.csproj/

  IntegrateNugetPackageJob(pipeline) {
    super(pipeline)
    gitConfig.with(DefaultValues.git)
    nexusConfig.with(DefaultValues.nexus)
    nugetConfig.with(DefaultValues.nuget)
    nexusConfig.apiKey.resolver = new PipelineTextValueResolver(pipeline)
    docker = new DockerTool(new PipelineShell(pipeline))
    fileFinder = new FindTool(new PipelineShell(pipeline))
  }

  def loadParameters(params) {
    gitConfig.branch = params?.sha1?.trim() ?: gitConfig.branch
  }

  def defineEnvVars() {
    super.defineEnvVars() +
    nugetConfig.defineEnvVars(pipeline).findAll()
  }

  def defineVolumes() {
    super.defineVolumes() +
    nugetConfig.defineVolumes(pipeline).findAll()
  }

  void initializeJob() {
    gitSourceRef = resolveSourceReference()
    if (!dockerRegistry) {
      dockerRegistry = new URI(nexusConfig.authority)
      dockerRegistry.userInfo = getUsernamePassword(nexusConfig.credsId).join(':')
    }
  }

  def resolveSourceReference() {
    new GitSourceReference(
      scheme: gitSourceRef.host
        ? gitSourceRef.scheme
        : (gitConfig.host ? gitConfig.protocol : gitSourceRef.scheme),
      host: gitSourceRef.host ?: gitConfig.host,
      owner: gitSourceRef.owner ?: gitConfig.owner,
      repository: gitSourceRef.repository,
      branch: gitSourceRef.branch ?: gitConfig.branch,
    )
  }

  void run() {
    stage('checkout') {
      source = checkout()
    }
    stage('study', source) {
      source += study()
    }
    stage('equip', source) {
      source += equip()
    }
    stage('build', source?.dockerfile) {
      image = build()
    }
    stage('test', image) {
      test()
    }
    stage('pack', image) {
      packet = pack()
    }
    stage('deliver', packet) {
      deliver()
    }
  }

  def checkout() {
    final source = checkoutSource(
      baseUrl: gitSourceRef.organizationUrl,
      repository: gitSourceRef.repository,
      branch: gitSourceRef.branch,
      credsId: gitConfig.credsId,
      pipeline: pipeline,
    )
    return [
      dir: [source.dir, gitSourceRef.directory].findAll().join('/')
    ].findAll { key, value -> value }
  }

  def study() {
    final findings = [
      projects: fileFinder.find(directory: source.dir, name: '*.csproj')
    ]
    final dockerfile = [source.dir, 'Dockerfile'].join('/')
    if (pipeline.fileExists(dockerfile)) {
      findings.dockerfile = dockerfile
    }
    findings.findAll { key, value -> value != null }
  }

  def equip() {
    final equipment = [
      nugetConfig: '.nuget.config'
    ]
    pipeline.sh(script:
      "cp '${ nugetConfig.configPath }' '${ [source.dir, equipment.nugetConfig].join('/') }'"
      .toString()
    )
    return equipment
  }

  def build() {
    final def (String username, String password) =
      (dockerRegistry?.userInfo?.split(':') ?: []) + [null]
    docker.login(
      server: dockerRegistry.toString(),
      username: username,
      password: password,
    )
    docker.build(
      path: source.dir,
      buildArgs: [
        nuget_conf_file: source.nugetConfig
      ],
      network: 'host',
    )
  }

  void test() {
    runInsideImage { shell ->
      shell.execute("""
        set -o errexit
        projects="\$(find . -regex '${ testProjectFilePattern }')"
        for project in "\${projects}"
        do
          dotnet test "\${project}"
        done
      """.stripIndent().trim())
    }
  }

  def pack() {
    packCsProjects(source.projects.collect {
      it.split('/').last()
    }).findAll {
      it.package
    }
  }

  void deliver() {
    deliverPackages(packet.collect {
      [
        name: it.project.split('/').last() - '.csproj',
        package: it.package,
        version: it.version,
      ]
    })
  }

  def checkoutSource(Map options) {
    CheckoutRecentSourceStep.newInstance(options).run()
  }

  def packCsProjects(List<String> projects) {
    runInsideImage { shell ->
      final dotnet = DotnetTool.newInstance(pipeline, shell)
      projects.collect {
        dotnet.csprojPack(it)
      }.findAll {
        it.package
      }.collect {
        it + [
          package: shell.downloadFile(it.package, '.')
        ]
      }
    }
  }

  void deliverPackages(List packages) {
    pushPackages(triagePackages(packages).findAll {
      it.exists == false }
    )
  }

  def triagePackages(List packages) {
    final repository = createNugetRepository()
    packages.collect {
      it + [
        exists: repository.hasNuget(
          name: it.name,
          version: it.version,
        )
      ]
    }
  }

  NugetRepository createNugetRepository() {
    final def (String user, String password) =
      getUsernamePassword(nexusConfig.credsId)
    NexusNugetRepository.newInstance(
      client: NexusClient.newInstance(
        pipeline, nexusConfig.authorityName,
        user, password
      )
    )
  }

  void pushPackages(List packages) {
    final nugetServer = [
      'https:/', nexusConfig.authorityName,
      'repository', 'debug-nugets',
    ].join('/')
    final nugetServerApiKey = nexusConfig.apiKey.toString()
    runInsideImage { shell ->
      final dotnet = DotnetTool.newInstance(pipeline, shell)
      packages.collect {
        it + [
          package: shell.uploadFile(it.package, '/tmp')
        ]
      }.each {
        dotnet.nugetPush(it.package,
          source: nugetServer,
          api_key: nugetServerApiKey,
        )
      }
    }
  }

  def runInsideImage(Closure code) {
    DockerContainerShell.newInstance(
      hostShell: PipelineShell.newInstance(pipeline),
      image: image,
      network: 'host',
    ).withShell(code)
  }

  def declare(Closure code) {
    code.delegate = new JobDeclaration(this)
    code.resolveStrategy = Closure.DELEGATE_ONLY
    code.call()
  }

  static class JobDeclaration {
    final IntegrateNugetPackageJob job

    JobDeclaration(IntegrateNugetPackageJob job) {
      this.job = job
    }

    void source(Closure code) {
      code.delegate = new SourceDeclaration(job)
      code.resolveStrategy = Closure.DELEGATE_ONLY
      code.call()
    }
  }

  static class SourceDeclaration {
    final IntegrateNugetPackageJob job

    SourceDeclaration(IntegrateNugetPackageJob job) {
      this.job = job
    }

    void git(String reference) {
      job.gitSourceRef = new GitSourceReference(reference)
    }

    void git(Map options) {
      job.gitSourceRef = new GitSourceReference(options)
    }

    void git(Closure code) {
      code.delegate = [:]
      code.call()
      job.gitSourceRef = new GitSourceReference(code.delegate)
    }
  }
}
