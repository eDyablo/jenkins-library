package com.e4d.job

import com.cloudbees.groovy.cps.NonCPS
import com.e4d.aws.sts.*
import com.e4d.build.*
import com.e4d.curl.*
import com.e4d.docker.*
import com.e4d.dotnet.*
import com.e4d.git.*
import com.e4d.ioc.*
import com.e4d.nexus.*
import com.e4d.nuget.*
import com.e4d.pip.*
import com.e4d.pipeline.*
import com.e4d.python.*
import com.e4d.service.*
import com.e4d.step.*
import com.e4d.swagger.*
import com.e4d.tar.*
import groovy.json.JsonOutput
import hudson.model.*

class IntegrateServiceJob extends PipelineJob {
  Boolean generateJobParameters = false
  DockerBuilder dockerBuilder
  DockerClient dockerClient
  GitConfig gitConfig = new GitConfig()
  NexusConfig nexusConfig = new NexusConfig()
  NugetConfig nugetConfig = new NugetConfig()
  PipConfig pipConfig = new PipConfig()
  ServiceConfig serviceConfig = new ServiceConfig()
  List<String> deploymentJobs = new ArrayList<String>()
  String swaggerPath
  Map values = [:]
  def source
  CurlTool curl
  TarTool tar
  String sourceRootDir

  final String unitTestProjects = /.*\.[Tt]ests?\.csproj/
  final String intgTestProjects = /.*\.[Ii]ntegration[Tt]ests?\.csproj/

  final pythonTestFileNamePatterns = [
    '*_test.py',
    'test_*.py',
  ]

  final pythonIntegrationTestFileNamePatterns = [
    '*_integration_test.py',
    'test_integration_*.py',
  ]

  IntegrateServiceJob(pipeline=ContextRegistry.context.pipeline) {
    super(pipeline)
    gitConfig.with(DefaultValues.git)
    nexusConfig.with(DefaultValues.nexus)
    nugetConfig.with(DefaultValues.nuget)
    pipConfig.with(DefaultValues.pip)
    curl = new CurlTool(pipeline)
    dockerBuilder = new DockerBuilder(pipeline)
    dockerClient = new DockerClient(pipeline)
    tar = new TarTool(pipeline)
  }

  @NonCPS
  void setDeploymentJob(String job) {
    deploymentJobs = job.split(',')*.trim().findAll { it }
  }

  @NonCPS
  void setDeploymentJob(List<String> jobs) {
    deploymentJobs = jobs
  }

  String getDeploymentJob() {
    deploymentJobs.join(', ')
  }

  Boolean hasDeploymentJob() {
    deploymentJobs && !deploymentJobs.isEmpty()
  }

  def defineParameters() {
    if (generateJobParameters) {
      [
        new StringParameterDefinition('deploymentJob', deploymentJob,
          'Name of job to trigger for deployment'),
        new StringParameterDefinition('swaggerPath', swaggerPath,
          'Name of swaggerPath')
      ] +
      gitConfig.defineParameters(pipeline) +
      nexusConfig.defineParameters(pipeline) +
      pipConfig.defineParameters(pipeline) +
      serviceConfig.defineParameters(pipeline)
    }
    else {
      []
    }
  }

  def loadParameters(params) {
    deploymentJob = params.deploymentJob ?: deploymentJob
    gitConfig.loadParameters(params)
    nexusConfig.loadParameters(params)
    pipConfig.loadParameters(params)
    serviceConfig.loadParameters(params)
  }

  def defineEnvVars() {
    super.defineEnvVars() +
    pipConfig.defineEnvVars(pipeline).findAll() +
    nugetConfig.defineEnvVars(pipeline).findAll()
  }

  def defineVolumes() {
    super.defineVolumes() +
    pipConfig.defineVolumes(pipeline).findAll() +
    nugetConfig.defineVolumes(pipeline).findAll()
  }

  void initializeJob() {
    gitConfig.repository = gitConfig.repository?.trim() ?: serviceConfig.service.sourceName
    serviceConfig.service.name = serviceConfig.service.name?.trim() ?: gitConfig.repository
  }

  def run() {
    checkout()
    final def (buildImage, serviceImage) = buildDockerImages(source)
    test(source, buildImage.id)
    testDockerImage(serviceImage, Path.combine(source.dir, 'integrate'))
    final deployment = buildDeploymentArtifact(source)
    publishDockerImages(source, buildImage, serviceImage)
    publishDeploymentArtifact(deployment)
    triggerDeployment(new ArtifactReference(deployment.name, deployment.tag))
  }

  void checkout() {
    stage('checkout') {
      source = checkoutSource()
      if (source.changedFiles.findAll()) {
        pipeline.writeFile(
          file: "${ source.dir }/.ci/changed-files",
          text: source.changedFiles.join('\n'),
          encoding: 'utf-8',
        )
      }
    }
  }

  def checkoutSource() {
    final source = runCheckoutStep()
    source.dir = [source.dir, sourceRootDir].findAll().join('/')
    if (!pipeline.fileExists(source.dir)) {
      pipeline.error("The directory '${ source.dir }' does not exist")
    }
    return source
  }

  def runCheckoutStep() {
    final step = new CheckoutRecentSourceStep(
      repository: gitConfig.repository,
      baseUrl: gitConfig.baseUrl,
      credsId: gitConfig.credsId,
      branch: pipeline.params.sha1 ?: gitConfig.branch,
    )
    step.run()
  }

  def buildDockerImages(source) {
    stage('build: docker image') {
      pipeline.dir(source.dir) {
        pipeline.sh("""\
          #!/usr/bin/env bash
          set -o errexit
          cp '${ pipConfig.configPath }' .pip.config
          cp '${ nugetConfig.configPath }' .nuget.config
        """.stripIndent())
      }
      final creds = [
        pipeline.usernamePassword(
          credentialsId: nexusConfig.credsId,
          usernameVariable: 'user',
          passwordVariable: 'password')
      ]
      pipeline.withCredentials(creds) {
        dockerBuilder.useRegistry(nexusConfig.authority, pipeline.env.user, pipeline.env.password)
      }
      final images = dockerBuilder.build(source.dir, network: 'host',
        build_arg: ['pip_conf_file=.pip.config',
          'nuget_conf_file=.nuget.config'])
      return images.size() > 1 ? images : images * 2
    }
  }

  def test(source, image) {
    try {
      stage('test: unit') {
        csharpUnitTest(image)
        pythonUnitTest(image)
      }
      stage('test: integration') {
        csharpIntegrationTest(image)
        pythonIntegrationTest(image)
      }
    }
    finally {
      publishTestResults()
    }
    stage('test: swagger') {
      testSwaggerSpec(source, image)
    }
  }

  def csharpUnitTest(image) {
    final pipelineShell = new PipelineShell(pipeline)
    final imageShell = new DockerImageShell(pipelineShell, image,
      network: 'host', env: getTempAwsCreds())
    final dotnet = new DotnetTool(pipeline, imageShell)
    dotnet.test('.', includeProjects: unitTestProjects, resultsDir: 'results')
  }

  def csharpIntegrationTest(image) {
    final pipelineShell = new PipelineShell(pipeline)
    final imageShell = new DockerImageShell(pipelineShell, image,
      network: 'host', env: getTempAwsCreds())
    final dotnet = new DotnetTool(pipeline, imageShell)
    dotnet.test('.', includeProjects: intgTestProjects, resultsDir: 'results')
  }

  def pythonUnitTest(image) {
    final pipelineShell = new PipelineShell(pipeline)
    final imageShell = new DockerImageShell(pipelineShell, image, network: 'host')
    final pytest = new PytestTool(pipeline, imageShell)
    pytest.test(includeFileNames: pythonTestFileNamePatterns,
      excludeFileNames: pythonIntegrationTestFileNamePatterns)
  }

  def pythonIntegrationTest(image) {
    final pipelineShell = new PipelineShell(pipeline)
    final imageShell = new DockerImageShell(pipelineShell, image,
      network: 'host', env: getTempAwsCreds())
    final pytest = new PytestTool(pipeline, imageShell)
    pytest.test(includeFileNames: pythonIntegrationTestFileNamePatterns)
  }

  def getTempAwsCreds() {
    if (values.service?.iamrole) {
      final pipelineShell = new PipelineShell(pipeline)
      final awsSts = new AwsStsTool(pipelineShell)
      final String region = 'us-west-2'
      final role = awsSts.assumeRole(
        "arn:aws:iam::429750608758:role/${ values.service?.iamrole }", 'ci',
        duration_seconds: 1800, region: region)
      return [ 
        "AWS_ACCESS_KEY_ID=\"${ role.Credentials.AccessKeyId }\"",
        "AWS_SECRET_ACCESS_KEY=\"${ role.Credentials.SecretAccessKey }\"",
        "AWS_SESSION_TOKEN=\"${ role.Credentials.SessionToken }\"",
        "AWS_DEFAULT_REGION=${ region }",
      ]
    }
    else {
      return []
    }
  }

  def testSwaggerSpec(source, image) {
    final tester = new SwaggerSpecTester(pipeline)
    tester.test("${ source.dir }", image, '/build/app/swagger.json')
  }

  def buildDeploymentArtifact(source) {
    stage('build: deployment artifact', hasDeploymentJob()) {
      saveDeploymentArtifactData(source)
      return packDeploymentArtifact(source)
    }
  }

  def saveDeploymentArtifactData(source) {
    final data = [
      version: 'v1',
      source: [
        snapshot: [
          hash: source.hash,
          revision: source.revision,
          tag: source.tag,
          timestamp: source.timestamp,
        ],
      ],
    ]
    pipeline.writeFile(
      file: "${ source.dir }/.ci/artifact-data", 
      text: JsonOutput.toJson(data),
      encoding: 'utf-8',
    )
  }

  def packDeploymentArtifact(source) {
    final tag = artifactVersionTag
    final String file = "${ serviceConfig.service.name }:${ tag }.tgz"
    tar.pack(file, source.dir, 'config', 'configs', 'deploy', '.ci',
      ignoreNonExisting: true)
    return [ file: file, name: serviceConfig.service.name, tag: tag ]
  }

  def testDockerImage(image, testDirectory) {
    stage('test: docker image') {
      final containerId = dockerClient.runCommand(image.id, detach: true)
      try {
        final containerIP = dockerClient.getContainerIPAddress(containerId)
        pipeline.withEnv(["SERVICE_PORT=${ containerIP }"]) {
          pipeline.sh("""\
            #!/usr/bin/env bash
            set -o errexit
            if [ -d "${ testDirectory }" ]; then
              tests=\$(find "${ testDirectory }" -regex ".*_test.sh")
              for test in \$tests
              do
                bash \$test
              done
            fi
          """.stripIndent())
        }
      }
      finally {
        final logs = dockerClient.getLogs(containerId, timestamps: true)
        dockerClient.stopContainer(containerId)
      }
    }
  }

  def publishTestResults() {
      pipeline.step([
        $class: 'MSTestPublisher',
        testResultsFile: "**/*.trx",
        failOnError: false,
        keepLongStdio: true
      ])
      if (pipeline.fileExists('results/coverage.cobertura.xml')) {
        pipeline.step([
          $class: 'CoberturaPublisher', 
          autoUpdateHealth: false, 
          autoUpdateStability: false, 
          coberturaReportFile: 'results/**.cobertura.xml', 
          failUnhealthy: false, 
          failUnstable: false, 
          maxNumberOfBuilds: 0, 
          onlyStable: false, 
          sourceEncoding: 'ASCII', 
          zoomCoverageChart: false
        ])
      } 
      else {
        pipeline.echo "There is no Cobertura reports "
      }
    }
    
  def publishDockerImages(source, buildImage, serviceImage) {
    stage('publish: docker image', hasDeploymentJob()) {
      final String tag = "${ nexusConfig.authority }/${ serviceConfig.service.name }:${ artifactVersionTag }"
      final String buildTag = "${ tag }-build"
      publishDockerImage(id: buildImage.id, tag: buildTag)
      publishDockerImage(id: serviceImage.id, tag: tag)
    }
  }

  def publishDockerImage(image) {
    pipeline.withCredentials([pipeline.usernamePassword(
      credentialsId: nexusConfig.credsId, usernameVariable: 'user',
      passwordVariable: 'password')]) {
      dockerClient.pushImage(image.id, image.tag, nexusConfig.authority,
        pipeline.env.user, pipeline.env.password)
    }
  }

  def publishDeploymentArtifact(artifact) {
    stage('publish: deployment artifact', hasDeploymentJob()) {
      final creds = [pipeline.usernamePassword(
        credentialsId: nexusConfig.credsId,
        usernameVariable: 'user',
        passwordVariable: 'password')]
      final url = new StringBuilder('https://') <<
        nexusConfig.authorityName <<
        '/repository/debug-artifacts/' <<
        [artifact.name, artifact.tag].join('/')
      pipeline.withCredentials(creds) {
        curl.uploadFile(
          file: artifact.file,
          url: url.toString(),
          user: pipeline.env.user,
          password: pipeline.env.password)
      }
    }
  }

  def triggerDeployment(artifact) {
    stage("deploy", hasDeploymentJob()) {
      final job = new DeployServiceJob(pipeline)
      job.artifact = artifact
      job.nexusConfig = nexusConfig
      final parameters = job.defineParameters().collect{ it.defaultParameterValue }
      deploymentJobs.drop(1).each {
        pipeline.build(job: it, parameters: parameters, wait: false)
      }
      deploymentJobs.take(1).each {
        final run = pipeline.build(
          job: it, parameters: parameters, wait: true, propagate: false)
        final message = "${ run.fullDisplayName } completed with status ${ run.result.toString() }"
        if (run.result == Result.UNSTABLE.toString()) {
          pipeline.unstable(message)
        } else if (run.result != Result.SUCCESS.toString()) {
          pipeline.error(message)
        }
      }
    }
  }

  @NonCPS
  SemanticVersion getSourceVersion() {
    new SemanticVersionBuilder().fromGitTag(source?.tag).build()
  }

  @NonCPS
  SemanticVersion getArtifactVersion() {
    def version = sourceVersion
    if (version.build) {
      final builder = new SemanticVersionBuilder()
        .major(version.major)
        .minor(version.minor)
        .patch(version.patch)
        .prerelease(version.prereleaseIds)
      if (source.timestamp) {
        builder.prerelease('sut', source.timestamp)
      }
      builder.build(version.buildIds)
      version = builder.build()
    }
    return version
  }

  @NonCPS
  String getArtifactVersionTag() {
    artifactVersion.toString().replace('+', '_')
  }
}
