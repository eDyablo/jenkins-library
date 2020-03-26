package com.e4d.job

import com.cloudbees.groovy.cps.NonCPS
import com.e4d.build.*
import com.e4d.curl.*
import com.e4d.docker.*
import com.e4d.dotnet.*
import com.e4d.ioc.*
import com.e4d.k8s.* 
import com.e4d.nexus.*
import com.e4d.nuget.*
import com.e4d.pipeline.*
import com.e4d.tar.*
import com.e4d.template.*
import groovy.json.JsonSlurperClassic
import groovy.util.GroovyCollections
import hudson.model.*

class DeployServiceJob extends PipelineJob {
  ArtifactReference _artifact
  DeployDestination destination = new DeployDestination()
  Map values = [:]
  NexusConfig nexusConfig = new NexusConfig()
  SecretReference k8sConfigRef = new SecretReference()

  Map options = [
    deploy: [
      completion: [
        timeout: 300,
        checkInterval: 10,
      ],
    ],
    testing: [
      skip: false,
    ],
  ]

  final CurlTool curl
  K8sClient k8s
  final K8sConfig k8sConfig = new K8sConfig()
  final TarTool tar
  final TemplateEngine templateEngine
  final k8sDirNames = ['k8s', 'kubernetes', 'deploy/k8s', 'deploy/kubernetes']
  final k8sFilePatterns = ['*.yaml', '*.json']
  final String deployTestProjects = /.*\.[Dd]eployment[Tt]ests?\.csproj/
  final Map cache = [
    serviceHost: null,
    serviceUrl: null,
  ]

  DeployServiceJob(pipeline=ContextRegistry.context.pipeline) {
    super(pipeline)
    curl = new CurlTool(pipeline)
    k8s = new K8sClient(pipeline)
    tar = new TarTool(pipeline)
    templateEngine = new TemplateEngine(pipeline)

    nexusConfig.with(DefaultValues.nexus)
    nexusConfig.apiKey.resolver = new PipelineTextValueResolver(pipeline)

    k8sConfigRef.with(DefaultValues.k8sConfigSecret)

    LinkedHashMap.metaClass.merge = { Map rhs ->
      def lhs = delegate
      rhs.each { k, v -> lhs[k] = lhs[k] in Map ? lhs[k].merge(v) : v }
      lhs
    }
  }

  ArtifactReference getArtifact() {
    _artifact
  }

  void setArtifact(ArtifactReference value) {
    _artifact = value
  }

  @NonCPS
  void setArtifact(String value) {
    _artifact = ArtifactReference.fromText(value)
  }

  @NonCPS
  void setK8sConfigRef(String value) {
    this.k8sConfigRef = SecretReference.fromText(value)
  }

  @NonCPS
  void setDestination(String value) {
    this.destination = DeployDestination.fromText(value)
  }

  def defineParameters() {
    [
      new StringParameterDefinition('artifact', artifact.toString(), 'Deployment artifact'),
      new StringParameterDefinition('destination', destination.pretty(),
        'Specify k8s context and namespace(optional) to deploy into. Use column separated string like \'context : namespace\'. Context must be present in k8s configuration specified'),
      new StringParameterDefinition('k8sConfigRef', k8sConfigRef.pretty(),
        'Specify k8s configuration used as reference to secret contains the configuration in form of column separated string \'secret-name : secret-key\'')
    ] +
    nexusConfig.defineParameters(pipeline)
  }

  def loadParameters(def params) {
    artifact = params.artifact ?: artifact.toString()
    destination = params.destination ?: destination.toString()
    k8sConfigRef = params.k8sConfigRef ?: k8sConfigRef.toString()
    nexusConfig.loadParameters(params)
  }

  void initializeJob() {
    k8sConfig.configRef = k8sConfigRef
    k8s.configPath = k8sConfig.configPath
  }

  def defineEnvVars() {
    k8sConfig.defineEnvVars(pipeline)
  }

  def defineVolumes() {
    k8sConfig.defineVolumes(pipeline)
  }

  def run() {
    pull()
    final def (files, dirs) = prepare()
    final deployment = deploy(files, dirs)
    try {
      deploy(getDeployers())
      test()
    }
    catch (any) {
      rollback(deployment)
      throw any
    }
    report()
  }

  def pull() {
    stage('pull: deployment artifact') {
      final file = "${ artifact.name }-${ artifact.tag }.tgz"
      final creds = [pipeline.usernamePassword(
        credentialsId: nexusConfig.credsId,
        usernameVariable: 'user',
        passwordVariable: 'password')]
      pipeline.withCredentials(creds) {
        curl.downloadFile(
          url: "https://${ nexusConfig.authorityName }/repository/debug-artifacts/${ artifact.name }/${ artifact.tag }",
          file: file,
          user: pipeline.env.user,
          password: pipeline.env.password
        )
      }
      tar.unpack(file)
      return file
    }
  }

  def getDeployers() {
    final String filename = 'deploy/publish-nuget-csproject'
    if (pipeline.fileExists(filename)) {
      readPublishNugetCSProjectScript(pipeline.readFile(filename))
    } else {
      []
    }
  }

  def readPublishNugetCSProjectScript(String script) {
    final declaration = new PublishNugetDeclaration()
    script.split('\n').each {
      declaration.csproject(it)
    }
    declaration.deployers
  }

  def prepare() {
    stage('prepare: apply values') {
      values.merge(
        deployment: [
          cluster: destination.context,
          namespace: destination.namespace
        ],
        service: [
          name: artifact.name,
          tag: artifact.tag,
          image: [nexusConfig.authority, [artifact.name, artifact.tag].join(':')].join('/')
        ]
      )

      final def (files, dirs) = findK8sFiles()
      
      setWriteAccess(dirs)

      applyValues(files)

      return [files, dirs]
    }
  }
  
  def setWriteAccess(dirs) {
    def commands = dirs.collect {
      "chmod -R a+w ${ it }"
    }
    pipeline.sh(commands.join('\n'))
  }
  
  def applyValues(files) {
    files.each {
      pipeline.echo("apply values to '${ it }'")
      final content = pipeline.readFile(file: it, encoding: 'utf-8').toString()

      final collector = new FilesCollector()
      templateEngine.render(content, values + [file: collector])

      final reader = new FilesContentReader(collector.files)
      reader.read(pipeline)
      final rendered = templateEngine.render(content, values + [file: reader])

      pipeline.writeFile(file: it, encoding: 'utf-8', text: rendered)
    }
  }

  def deploy(files, dirs) {
    stage('deploy: apply k8s resources') {
      final bundle = new ManifestFileSorter().triage(files, prefixes: dirs).findAll {
        it.namespace != 'monitoring'
      }
      final String backupFile = 'applied-config-backup.yaml'

      k8s.createNamespace(extractNamespaces(bundle))
      createDockerRegistrySecret()

      def resources
      try {
        resources = apply(bundle, dry_run: true)
      }
      catch (any) {
        files.each {
          final content = pipeline.readFile(file: it, encoding: 'utf-8')
          pipeline.echo([it, content].join('\n'))
        }
        throw any
      }

      final String noValue = '<no value>'
      final def (original, missing) = resources.split {
          it.creationTimestamp && it.creationTimestamp != noValue
      }

      if (missing) {
        pipeline.echo("Create the following resources:\n${ missing.join('\n') }")
      }

      if (original) {
        pipeline.echo("Update the following resources:\n${ original.join('\n') }")
        final appliedConfig = k8s.getAppliedConfig(original,
          namespace: destination.namespace, output: 'yaml')
        pipeline.writeFile(file: backupFile,
          encoding: 'utf-8', text: appliedConfig)
      }

      try {
        final configured = apply(bundle)
        if (configured) {
          pipeline.echo("The following resources have been configured:\n${ configured.join('\n') }")
        }

        final deployments = configured.findAll { it.kind.toLowerCase() == 'deployment' }
        deployments.each {
          pipeline.echo("wait for '${ it.name }' deployment to complete")
          k8s.waitDeployment(it.name, namespace: it.namespace,
            completionTimeout: options.deploy.completion.timeout,
            checkInterval: options.deploy.completion.checkInterval)
        }

        return [
          original : original,
          created: missing,
          configured: configured,
          backup: backupFile]
      }
      catch (any) {
        rollback(original : original, created: missing, backup: backupFile)
        throw any
      }
    }
  }

  def extractNamespaces(List<ManifestFileSet> bundle) {
    bundle.collect{ it.namespace }.findAll{ it } + destination.namespace
  }

  def apply(Map options=[:], List<ManifestFileSet> bundle) {
    bundle.inject([]) { applied, manifest ->
      options.namespace = manifest.namespace ?: destination.namespace
      applied + k8s.apply(options, manifest.paths)
    }
  }

  def test() {
    stage('test: deployment', options.testing.skip == false) {
      pipeline.echo([
        ingressUrl: ingressUrl,
        serviceHostUrl: serviceHost.url,
        serviceSelfLink: serviceSelfLink,
        serviceUrl: serviceUrl,
        serviceEndpoint: serviceEndpoint,
      ]
      .collect { key, value ->
        "${ key }: ${ value }"
      }
      .join('\n'))
      String serviceHostUrl = serviceHost.url
      if (serviceHostUrl.startsWith('https://')) {
        serviceHostUrl = serviceHostUrl[8..-1]
      }
      final env = [
        "SERVICE_HOST=${ serviceHostUrl }",
        "SERVICE_HOST_CREDS=${ serviceHost.creds }",
        "SERVICE_URL=${ serviceUrl }",
        "SERVICE_ENDPOINT=${ serviceEndpoint }",
      ]
      pythonTest(env)
      dotnetTest(env)
    }
  }

  def pythonTest(env) {
    pipeline.withEnv(env) {
      pipeline.sh("""\
        #!/usr/bin/env bash
        pip install pytest && sh -c 'python -m pytest; ret=\$?; [ \$ret = 5 ] && exit 0 || exit \$ret'
      """.stripIndent())
    }
  }

  def dotnetTest(env) {
    pipeline.withCredentials([pipeline.usernamePassword(
      credentialsId: nexusConfig.credsId, usernameVariable: 'user',
      passwordVariable: 'password')]) {
      final image = [nexusConfig.authority, [artifact.name,
        [artifact.tag, 'build'].join('-')].join(':')].join('/')
      final shell = new DockerImageShell(new PipelineShell(pipeline), image,
        registry: nexusConfig.authority , user: pipeline.env.user,
        password: pipeline.password, env: env, network: 'host')
      final dotnet = new DotnetTool(pipeline, shell)
      dotnet.test('.', includeProjects: deployTestProjects)
    }
  }

  def report() {
    stage('report') {
      pipeline.echo(buildReport())
    }
  }

  def rollback(deployment) {
    if (deployment.original && deployment.backup) {
      final rolledback = k8s.apply([deployment.backup])
      if (rolledback) {
        pipeline.echo("The following resources have been rolled back:\n${ rolledback.join('\n') }")
      }
    }

    if (deployment.created) {
      pipeline.echo("Delete the following resources:\n${ deployment.created.join('\n') }")
      k8s.delete(deployment.created, ignore_not_found: true)
    }
  }

  def getServiceHost() {
    if (cache.serviceHost == null) {
      if (ingressUrl) {
        cache.serviceHost = [url: ingressUrl, creds: 'none']
      } else {
        cache.serviceHost = k8s.getServer(destination.context)
      }
    }
    return cache.serviceHost
  }

  def getServiceUrl() {
    if (cache.serviceUrl == null) {
      if (ingressUrl) {
        cache.serviceUrl = [
          '',
          destination.namespace,
          artifact.name,
        ].join('/')
      } else {
        cache.serviceUrl = [
          serviceSelfLink,
          'http/proxy',
        ].join(':')
      }
    }
    return cache.serviceUrl
  }

  def getServiceEndpoint() {
    final host = serviceHost.url
    final service = serviceUrl
    if (host.endsWith('/')) {
      host = host[0..-2]
    }
    if (service.startsWith('/')) {
      service = service[1..-1]
    }
    [host, service].join('/')
  }

  String getServiceSelfLink() {
    if (cache.serviceSelfLink == null) {
      final link = k8s.getResourceData('service', [artifact.name],
        namespace: destination.namespace,
        output: 'go-template={{ .metadata.selfLink }}'
      ) ?: ''
      cache.serviceSelfLink = link.contains('<no value>') ? '' : link
    }
    return cache.serviceSelfLink
  }

  String getIngressUrl() {
    if (cache.ingressUrl == null) {
      final url = k8s.getResourceData('service', ['e4d-rproxy'],
        namespace: 'kube-system',
        output: 'go-template',
        template: '{{ index .metadata.annotations "external-dns.alpha.kubernetes.io/hostname" }}',
        ignore_not_found: true,
      ) ?: ''
      cache.ingressUrl = url.contains('<no value>') ? '' : url
    }
    return cache.ingressUrl
  }

  def findFiles(String directory, String name) {
    pipeline.sh(
      script: """\
        #!/usr/bin/env bash
        find ${ directory } -name '${ name }'
      """.stripIndent(),
      returnStdout: true,
      encoding: 'utf-8'
    ).split('\n') as List<String>
  }

  def findK8sFiles() {
    final k8sDirs = k8sDirNames.findAll{ pipeline.fileExists(it) }
    final selectors = GroovyCollections.combinations([k8sDirs, k8sFilePatterns])
    final files = selectors.inject([]) { accumulator, selector ->
      accumulator += findFiles(selector[0], selector[1])
      accumulator
    }
    return [ files.findAll{ it }, k8sDirs ]
  }

  def deploy(List<Deployer> deployers) {
    final context = createDeploymentContext()
    final toDeploy = deployers.findAll {
      it.canDeploy(context)
    }
    stage('deploy: extra', !toDeploy.isEmpty()) {
      try {
        toDeploy.each {
          it.deploy(context)
        }
      } finally {
        context.shell.exit()
      }
    }
  }

  def createDeploymentContext() {
    pipeline.withCredentials(nexusCreds) {
      final shell = new DockerContainerShell(
        hostShell: new PipelineShell(pipeline),
        network: 'host',
        image: buildImage,
        registry: nexusConfig.authority,
        registryCreds: [
          username: pipeline.env.user,
          password: pipeline.env.password,
        ],
        labels: [
          originator: [this.class.name, hashCode()].join('-'),
        ],
      )
      return [
        pipeline: pipeline,
        shell: shell,
        nuget: [
          server: "https://${ nexusConfig.authorityName }/repository/debug-nugets",
          api_key: nexusConfig.apiKey.toString(),
        ],
        nugetRepository: nugetRepository,
        changedFiles: changedFiles,
        artifactVersion: artifactVersion,
      ]
    }
  }

  def getChangedFiles() {
    final String path = '.ci/changed-files'
    if (pipeline.fileExists(path)) {
      pipeline.readFile(path).split('\n')
    }
  }

  SemanticVersion getArtifactVersion() {
    final artifact = artifactData
    def version = sourceVersion
    if (version.build) {
      final builder = new SemanticVersionBuilder()
        .major(version.major)
        .minor(version.minor)
        .patch(version.patch)
        .prerelease(version.prereleaseIds)
      if (artifact.source?.snapshot?.timestamp) {
        builder.prerelease('sut', artifact.source.snapshot.timestamp)
      }
      builder.build(version.buildIds)
      version = builder.build()
    }
    return version
  }

  SemanticVersion getSourceVersion() {
    final String tag = artifactData.source?.snapshot?.tag
      ?: artifact?.tag
    new SemanticVersionBuilder().fromGitTag(tag).build()
  }

  def getArtifactData() {
    final String path = '.ci/artifact-data'
    if (pipeline.fileExists(path)) {
      final String content = pipeline.readFile(path)
      if (content) {
        return new JsonSlurperClassic().parseText(content)
      }
    }
    return [:]
  }
  
  def getNexusCreds() {
    [pipeline.usernamePassword(
      credentialsId: nexusConfig.credsId,
      usernameVariable: 'user',
      passwordVariable: 'password')]
  }

  def getBuildImage() {
    [nexusConfig.authority, [artifact.name,
      [artifact.tag, 'build'].join('-')].join(':')].join('/')
  }

  def buildReport() {
    try {
      final apis = [
        healthy: 'control/healthy',
        ready: 'control/ready',
        swagger: 'swagger',
      ]
      apis.collect { name, route ->
        [serviceEndpoint, route].join('/')
      }.join('\n')
    }
    catch(error) {
      "Failed to build the report.\n${ error }"
    }
  }

  void createDockerRegistrySecret() {
    final creds = [pipeline.usernamePassword(
      credentialsId: nexusConfig.credsId, usernameVariable: 'user',
      passwordVariable: 'password')]
    pipeline.withCredentials(creds) {
      k8s.context = destination.context
      k8s.createDockerRegistrySecret(
        name: 'regsecret',
        namespace: destination.namespace,
        server: nexusConfig.authority,
        user: pipeline.env.user,
        password: pipeline.env.password,
      )
    }
  }

  NugetRepository getNugetRepository() {
    return new NexusNugetRepository(client: nexusClient)
  }

  NexusClient getNexusClient() {
    final def (String user, String password) = nexusUserAndPassword
    return new NexusClient(pipeline, nexusConfig.authorityName, user, password)
  }

  def getNexusUserAndPassword() {
    pipeline.withCredentials(nexusCreds) {
      return [pipeline.env.user, pipeline.env.password]
    }
  }
}
