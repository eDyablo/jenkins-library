package com.e4d.job

import com.e4d.aws.ec2.AwsEc2MetadataReader
import com.e4d.build.SemanticVersion
import com.e4d.build.SemanticVersionBuilder
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
  boolean publishPrereleaseVersion = true
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
  String releaseVersionPattern = /^\d+(\.\d+){2}$/
  String testProjectFilePattern = /.+\.[Tt]ests?\.csproj/
  String testResultDir = [this.class.simpleName, hashCode()].join('-')
  URI dockerRegistry

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
    if (params?.sha1?.trim()) {
      gitSourceRef = gitSourceRef.withBranch(params.sha1.trim())
    }
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
    stage('checkout', gitSourceRef.isValid) {
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
    source.dir = [source.dir, gitSourceRef.directory].findAll().join('/')
    source.findAll { key, value -> value }
  }

  def study() {
    final findings = [
      projects: fileFinder.find(directory: source.dir, name: '*.csproj'),
      version: determineSourceVersion(source)
    ]
    final dockerfile = [source.dir, 'Dockerfile'].join('/')
    if (pipeline.fileExists(dockerfile)) {
      findings.dockerfile = dockerfile
    }
    findings.findAll { key, value -> value != null }
  }

  private SemanticVersion determineSourceVersion(source) {
    final version = new SemanticVersionBuilder().fromGitTag(source.tag).build()
    if (version.build) {
      final builder = new SemanticVersionBuilder()
        .major(version.major)
        .minor(version.minor)
        .patch(version.patch)
        .prerelease(version.prereleaseIds)
      if (source.timestamp) {
        builder.prerelease('sut', source.timestamp)
      }
      builder.prerelease(version.buildIds)
      version = builder.build()
    }
    return version
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
    final awsCreds = readAwsSecurityCredentials()
    final envs = [
      AWS_ACCESS_KEY_ID: awsCreds.AccessKeyId,
      AWS_SECRET_ACCESS_KEY: awsCreds.SecretAccessKey,
      AWS_SESSION_TOKEN: awsCreds.Token,
    ].collect { k, v -> [k, v].join('=') }
    try {
      runInsideImage { shell ->
        try {
          shell.execute(envs,
            script: """
              set -o errexit
              mkdir -p '${ testResultDir }'
              projects="\$(find . -regextype egrep -regex '${ testProjectFilePattern }')"
              if [ -n "\${projects}" ]
              then
                for project in \${projects}
                do
                  dotnet test "\${project}" --logger trx --results-directory '${ testResultDir }'
                done
              fi
            """.stripIndent().trim())
        } finally {
          shell.downloadFile(testResultDir, '.')
        }
      }
    } finally {
      publishTestResults(testResultDir)
    }
  }

  Map readAwsSecurityCredentials() {
    AwsEc2MetadataReader.newInstance(PipelineShell.newInstance(pipeline))
      .readSecurityCredentials()
  }

  void publishTestResults(String directory) {
    pipeline.step([
      $class: 'MSTestPublisher',
      testResultsFile: [directory, '*.trx'].join('/'),
      failOnError: false,
      keepLongStdio: true
    ])
  }

  def pack() {
    def packages = packCsProjects(source.projects.collect {
      it.split('/').last()
    }).findAll { pkg ->
      pkg.nugets
    }
    if (publishPrereleaseVersion == false) {
      packages = packages.findAll { pkg ->
        pkg.version =~ releaseVersionPattern
      }
    }
    return packages
  }

  void deliver() {
    deliverPackages(packet.collect { pkg ->
      [
        name: pkg.project.split('/').last() - '.csproj',
        nugets: pkg.nugets,
        version: pkg.version,
      ]
    })
  }

  def checkoutSource(Map options) {
    CheckoutRecentSourceStep.newInstance(options).run()
  }

  def packCsProjects(List<String> projects) {
    runInsideImage { shell ->
      final dotnet = DotnetTool.newInstance(pipeline, shell)
      projects.collect { project ->
        dotnet.csprojPack(project, version: source.version)
      }.findAll { pkg ->
        pkg.nugets
      }.collect { pkg ->
        pkg + [
          nugets: pkg.nugets.findAll().collect { nuget ->
            shell.downloadFile(nuget, '.')
          }
        ]
      }
    }
  }

  void deliverPackages(List packages) {
    pushPackages(triagePackages(packages).findAll { !it.exists })
  }

  def triagePackages(List packages) {
    final repository = createNugetRepository()
    packages.collect { pkg ->
      pkg + [
        exists: repository.hasNuget(
          name: pkg.name,
          version: pkg.version,
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
    final nugetSymbolServer = [
      'https:/', nexusConfig.authorityName,
      'repository', 'debug-nuget-symbol',
    ].join('/')
    final nugetServerApiKey = nexusConfig.apiKey.toString()
    final nugetSymbolServerApiKey = nugetServerApiKey
    runInsideImage { shell ->
      final dotnet = DotnetTool.newInstance(pipeline, shell)
      packages.collect { pkg ->
        pkg + [
          nugets: pkg.nugets.collect { nuget ->
            shell.uploadFile(nuget, '/tmp')
          }
        ]
      }.each { pkg ->
        pkg.nugets.findAll { nuget ->
          nuget.endsWith('.nupkg') && !nuget.endsWith('.symbols.nupkg')
        }.each { nuget ->
          dotnet.nugetPush(nuget,
            source: nugetServer,
            api_key: nugetServerApiKey,
            symbolSource: nugetSymbolServer,
            symbolApiKey: nugetSymbolServerApiKey,
          )
        }
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

    void translate(Class declaration, Closure code) {
      code.delegate = declaration.newInstance(job)
      code.resolveStrategy = Closure.DELEGATE_ONLY
      code.call()
    }

    void source(Closure code) {
      translate(SourceDeclaration, code)
    }

    void publish(Closure code) {
      translate(PublishDeclaration, code)
    }

    void publishStrategy(Closure code) {
      translate(PublishStrategyDeclaration, code)
    }

    def getPublishStrategy() {
      PublishStrategyDeclaration.newInstance(job)
    }

    void testing(Closure code) {
      translate(TestingDeclaration, code)
    }

    def getTesting() {
      TestingDeclaration.newInstance(job)
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

  static class PublishDeclaration {
    final IntegrateNugetPackageJob job

    PublishDeclaration(IntegrateNugetPackageJob job) {
      this.job = job
    }

    def getStrategy() {
      PublishStrategyDeclaration.newInstance(job)
    }

    void strategy(Closure code) {
      code.delegate = PublishStrategyDeclaration.newInstance(job)
      code.resolveStrategy = Closure.DELEGATE_ONLY
      code.call()
    }
  }

  static class PublishStrategyDeclaration {
    final IntegrateNugetPackageJob job

    PublishStrategyDeclaration(IntegrateNugetPackageJob job) {
      this.job = job
    }

    def getSkipPrereleaseVersion() {
      job.publishPrereleaseVersion = false
    }
  }

  static class TestingDeclaration {
    final IntegrateNugetPackageJob job

    TestingDeclaration(IntegrateNugetPackageJob job) {
      this.job = job
    }

    void projectFilePattern(String pattern) {
      setProjectFilePattern(pattern)
    }

    void setProjectFilePattern(String pattern) {
      job.testProjectFilePattern = pattern
    }
  }
}
