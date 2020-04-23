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

class IntegrateHelmChartJob extends IntegrateJob {
  def chart
  FileHub fileHub
  HelmTool helm
  NexusConfig nexusConfig = new NexusConfig()

  IntegrateHelmChartJob(pipeline=ContextRegistry.context.pipeline) {
    super(pipeline)
    nexusConfig.with(DefaultValues.nexus)
    helm = new HelmTool(new PipelineShell(pipeline))
    fileHub = new CurlFileHub(new PipelineShell(pipeline))
  }

  void run() {
    stage('checkout') {
      checkout()
    }
    stage('study') {
      source += study()
    }
    stage('test', source.chartMetadataFile) {
      testChart()
    }
    stage('package', source.chartMetadataFile) {
      packageChart()
    }
    stage('publish', chart) {
      publishChart()
    }
    stage('deploy', chart) {
      deployChart()
    }
  }

  def study() {
    final chartMetadataFile = [source.dir, 'Chart.yaml'].join('/')
    if (pipeline.fileExists(chartMetadataFile)) {
      return [chartMetadataFile: chartMetadataFile]
    } else {
      pipeline.unstable("chart metadata (Chart.yaml) is not found in '${ source.dir }'")
      return [:]
    }
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
}
