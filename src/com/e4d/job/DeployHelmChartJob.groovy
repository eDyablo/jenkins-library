package com.e4d.job

import com.cloudbees.groovy.cps.NonCPS
import com.e4d.curl.CurlFileHub
import com.e4d.helm.HelmTool
import com.e4d.k8s.K8sConfig
import com.e4d.nexus.NexusConfig
import com.e4d.pipeline.PipelineShell
import com.e4d.secret.PipelineSecret
import com.e4d.secret.SecretTextBearer
import com.e4d.template.MapMerger
import java.net.URI

class DeployHelmChartJob extends PipelineJob {
  def fetchedChart
  DeployDestination destination = new DeployDestination()
  HelmTool helm
  K8sConfig k8sConfig = new K8sConfig()
  List<String> valueFiles = []
  Map chartValues = [:]
  NexusConfig nexusConfig = new NexusConfig()
  URI chartURI

  DeployHelmChartJob(Map options=[:], pipeline) {
    super(pipeline)
    helm = options.helm ?: new HelmTool(new PipelineShell(pipeline))
    nexusConfig.with(DefaultValues.nexus)
    k8sConfig.with(DefaultValues.k8s)
  }

  def loadParameters(params) {
    if (params.helmChart) {
      chart = params.helmChart.toString()
    }
  }

  void run() {
    stage('fetch') {
      fetchChart()
    }
    stage('deploy') {
      deployChart()
    }
  }

  def defineVolumes() {
    k8sConfig.defineVolumes(pipeline)
  }

  void fetchChart() {
    final chartURI = chart
    final def (String user, String password) =
      (chartURI.userInfo?.split(':') ?: []) + [null]
    fetchedChart = helm.fetchChart(
      chartURL: chartURI.toString(),
      user: user,
      password: password,
      unpack: true,
    )
  }

  void setChart(URI value) {
    chartURI = value
  }

  void setChart(String value) {
    chartURI = new URI(value)
  }

  void setDestination(String value) {
    this.destination = DeployDestination.fromText(value)
  }

  @NonCPS
  void setDestination(DeployDestination value) {
    this.destination = value
  }

  URI getChart() {
    final baseURI = chartRepositoryURI
    def uri = chartRepositoryURI.resolve(chartURI ?: '')
    uri = new URI(
      uri.scheme,
      (String)null,
      uri.host,
      uri.port,
      uri.path,
      uri.query,
      uri.fragment
    )
    uri.userInfo = chartURI?.userInfo ?: chartRepositoryURI.userInfo
    return uri
  }

  URI getChartRepositoryURI() {
    String baseUrl = nexusConfig.baseUrl
    if (baseUrl && !baseUrl.endsWith('/')) {
      baseUrl += '/'
    }
    final uri = new URI(baseUrl + 'repository/charts/')
    uri.userInfo = getUsernamePassword(nexusConfig.credsId)
      .findAll().join(':') ?: null
    return uri
  }

  String getReleaseName() {
    [destination.namespace, fetchedChart.name].findAll {
      it?.trim() && it != 'default'
    }.join('-')
  }

  void deployChart() {
    helm.upgradeChartRelease(
      releaseName, fetchedChart.path,
      kubeConfig: k8sConfig.configPath,
      kubeContext: destination.context,
      namespace: destination.namespace,
      install: true,
      valueFiles: prepareChartValueFiles(),
      wait: true,
    )
  }

  def prepareChartValueFiles() {
    valueFiles.collect {
      [fetchedChart.path, it].join('/')
    } + storeChartValues()
  }

  def storeChartValues() {
    final def files = []
    if (chartValues) {
      final file = 'values'
      pipeline.writeYaml(
        file: file,
        data: declassified(chartValues),
      )
      files << file
    }
    return files
  }

  def declassified(Map values) {
    values.inject([:]) { result, entry ->
      if (entry.value instanceof PipelineSecret) {
        result[entry.key] = entry.value.declassify(pipeline)
      } else if (entry.value instanceof Map) {
        result[entry.key] = declassified(entry.value)
      } else {
        result[entry.key] = entry.value
      }
      result
    }
  }

  def declare(Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_ONLY
    closure.delegate = new Declaration(this)
    closure.call()
  }

  static class Declaration {
    final DeployHelmChartJob job

    Declaration(DeployHelmChartJob job) {
      this.job = job
    }

    void chart(String value) {
      job.chart = value
    }

    void destination(String value) {
      job.destination = value
    }

    void values(Closure code) {
      code.resolveStrategy = Closure.DELEGATE_ONLY
      code.delegate = new ValuesDeclaration(
        files: job.valueFiles,
        values: job.chartValues,
      )
      code.call()
    }
  }

  static class ValuesDeclaration {
    final List<String> files
    final Map values
    final merger = new MapMerger()

    ValuesDeclaration(Map options=[:]) {
      files = options.files
      values = options.values
    }

    void fromFile(String path) {
      files << path
    }

    void fromValues(Map map) {
      values << merger.merge(values, map)
    }

    void from(String arg) {
      fromFile(arg)
    }

    void from(Map arg) {
      fromValues(arg)
    }

    def secretText(String credsId) {
      new SecretTextBearer(credsId)
    }
  }
}
