package com.e4d.job

import com.e4d.build.SemanticVersion
import com.e4d.build.SemanticVersionBuilder
import com.e4d.curl.CurlFileHub
import com.e4d.file.FileHub
import com.e4d.git.GitConfig
import com.e4d.helm.HelmTool
import com.e4d.ioc.*
import com.e4d.nexus.NexusConfig
import com.e4d.pipeline.PipelineShell
import com.e4d.step.*
import hudson.model.*
import java.net.URI

class IntegrateHelmChartJob extends PipelineJob {
  def chart
  def source
  FileHub fileHub
  GitConfig gitConfig = new GitConfig()
  HelmTool helm
  NexusConfig nexusConfig = new NexusConfig()
  String sourceRoot

  IntegrateHelmChartJob(pipeline=ContextRegistry.context.pipeline) {
    super(pipeline)
    gitConfig.with(DefaultValues.git)
    nexusConfig.with(DefaultValues.nexus)
    helm = new HelmTool(new PipelineShell(pipeline))
    fileHub = new CurlFileHub(new PipelineShell(pipeline))
  }

  def loadParameters(params) {
    if (params?.sha1?.trim()) {
      gitConfig.branch = params.sha1
    }
  }

  void run() {
    stage('checkout') {
      checkout()
    }
    stage('test') {
      testChart()
    }
    stage('package') {
      packageChart()
    }
    stage('publish') {
      publishChart()
    }
    stage('deploy') {
      deployChart()
    }
  }

  void checkout() {
    source = checkoutSource(
      pipeline: pipeline,
      baseUrl: gitConfig.baseUrl,
      repository: gitConfig.repository,
      branch: gitConfig.branch,
      credsId: gitConfig.credsId,
    )
    source.dir = [source.dir, sourceRoot].findAll {
      it?.trim()
    }.join('/')
    if (!pipeline.fileExists(source.dir)) {
      pipeline.error("The directory '${ source.dir }' does not exist")
    }
  }

  def checkoutSource(Map options) {
    new CheckoutRecentSourceStep(options).run()
  }

  void packageChart() {
    chart = helm.packageChart(
      chartPath: source.dir,
      version: artifactVersion.toString(),
      updateDependencies: true,
    )
  }

  void testChart() {
    helm.lintChart(path: source.dir)
  }

  SemanticVersion getSourceVersion() {
    new SemanticVersionBuilder().fromGitTag(source?.tag).build()
  }

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

  URI getChartRepositoryURI() {
    final baseURI = new URI(nexusConfig.baseUrl)
    final uri = new URI()
    uri.scheme = baseURI.scheme
    uri.authority = baseURI.authority
    uri.path = baseURI.path + '/repository/charts/'
    uri.userInfo = getUsernamePassword(nexusConfig.credsId).join(':')
    return uri
  }

  void publishChart() {
    final String userInfo = chartRepositoryURI.userInfo ?: ''
    final def (String user, String password) = userInfo.split(':') + ['']
    fileHub.uploadFile(
      file: chart.path,
      destination: chartRepositoryURI.toString(),
      user: user,
      password: password,
    )
  }

  void deployChart() {
    final chartFile = chart.path?.split('/')?.last()
    pipeline.build(
      job: 'deploy',
      parameters: [
        new StringParameterValue('helmChart', chartFile),
      ],
      wait: true,
    )
  }

  def declare(Closure code) {
    code.delegate = new JobDeclaration(this)
    code.resolveStrategy = Closure.DELEGATE_ONLY
    code.call()
  }

  static class JobDeclaration {
    final IntegrateHelmChartJob job

    JobDeclaration(IntegrateHelmChartJob job) {
      this.job = job
    }

    void source(Closure code) {
      code.delegate = new SourceDeclaration(job)
      code.resolveStrategy = Closure.DELEGATE_ONLY
      code.call()
    }
  }

  static class SourceDeclaration {
    final IntegrateHelmChartJob job

    SourceDeclaration(IntegrateHelmChartJob job) {
      this.job = job
    }

    void setGitRepository(String repository) {
      job.gitConfig.repository = repository
    }

    void setRoot(String path) {
      job.sourceRoot = path
    }
  }
}
