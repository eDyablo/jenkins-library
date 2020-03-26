package com.e4d.job

import com.e4d.build.TemplateEngine
import com.e4d.curl.CurlTool
import com.e4d.git.GitConfig
import com.e4d.k8s.K8sClient
import com.e4d.k8s.K8sConfig
import com.e4d.nexus.NexusConfig
import com.e4d.template.MapMerger


class PerformanceTestJob extends PipelineJob {

  GitConfig gitConfig = new GitConfig()
  K8sConfig k8sConfig = new K8sConfig()
  NexusConfig nexusConfig = new NexusConfig()
  final CurlTool curl
  K8sClient k8sClient

  String loadGenerator = 'taurus' // Supported load generator tools: taurus, locust
  String repoName                 // Repository name for specific microservice
  String branch = 'develop'       // Specific branch name
  String context                  // dev, experimental or prod
  String taurusConfig = 'configs/load_test.yml'
  String reportDir = 'test_artifacts'
  String options = '' // options example: '-report'
  String xmlReport = 'test_artifacts/locust-report.xml'
  String csvReport = 'test_artifacts/locust-report.csv'
  String locustFile = 'locust_scenario.py'
  String locustClass = ''
  String host
  Integer numClients
  Integer hatchRate
  String testTimeout
  String logLevel = 'INFO'
  String logFileName // = 'locust'
  String reportFileName = 'locust-report'
  String grafanaUri
  String grafanaServiceName
  String grafanaServiceNamespace = 'default'
  String locustReportingLibVersion = '0.1.7'

  Map taurus = [:]
  Map prometheus = [:]
  final TemplateEngine templateEngine

  PerformanceTestJob(pipeline) {
    super(pipeline)
    k8sClient = new K8sClient(pipeline)
    k8sConfig.with(DefaultValues.k8s)
    gitConfig.with(DefaultValues.git)
    nexusConfig.with(DefaultValues.nexus)
    curl = new CurlTool(pipeline)
    gitConfig.branch = branch
    templateEngine = new TemplateEngine(pipeline)
  }

  def defineEnvVars() {
    k8sConfig.defineEnvVars(pipeline)
  }

  def defineVolumes() {
    k8sConfig.defineVolumes(pipeline)
  }

  void initializeJob() {
    k8sClient.configPath = k8sConfig.configPath
    k8sClient.context = context ?: 'default'
  }

  def run(){
    initEnvVars(pipeline)
    installLocustJenkinsReporterLib()
    checkoutGitRepo(repoName, branch)

    if (loadGenerator == 'taurus') {
      if (taurus) {
        taurusConfig = 'configs/load_test_template.yml'
        copyTaurusTemplate(taurusConfig)
        prepare()
      }
      runBlazemeterTaurus(
        taurusConfig,
        options,
        xmlReport,
        csvReport
      )
    } else if (loadGenerator == 'locust') {
      runLocust(
        locustFile,
        locustClass,
        host,
        numClients,
        hatchRate,
        testTimeout,
        logFileName,
        logLevel ?: "INFO",
        reportFileName ?: "locust-report"
      )
    } else {
      return pipeline.error("Framework: $loadGenerator - not supported!!!")
    }
  }

  def initEnvVars(pipeline) {
    pipeline.env.REPORT_DIR = reportDir
    if (prometheus) {
      pipeline.env.PROMETHEUS_URL = prometheus.url
      pipeline.env.PROMETHEUS_NAMESPACE = prometheus.namespace
      pipeline.env.PROMETHEUS_SERVICE = prometheus.service
      pipeline.env.PROMETHEUS_POD = prometheus.pod
    }
    if (grafanaUri) { pipeline.env.MONITORING_BASE_URI = grafanaUri }
    if (grafanaServiceName) { pipeline.env.MONITORING_SERVICE = grafanaServiceName}
    if (grafanaServiceNamespace) {pipeline.env.MONITORING_NAMESPACE = grafanaServiceNamespace}
  }

  def copyTaurusTemplate(configFilePath) {
    pipeline.sh 'mkdir -p load_test/configs'
    pipeline.echo "Writing Taurus template to $configFilePath"
    def file = pipeline.libraryResource 'com/e4d/locust/load_test_template.yml'
    pipeline.writeFile file: "load_test/$configFilePath", encoding: 'utf-8', text: file
  }

  def prepare() {
    new MapMerger().merge(taurus, [
      locustScenario: taurus.locustScenario ?: '../locust_scenario.py'
    ])
    def content = pipeline.readFile(file: "load_test/$taurusConfig", encoding: 'utf-8').toString()
    def rendered = templateEngine.render(content, taurus)
    pipeline.echo(rendered)
    pipeline.writeFile(file: "load_test/$taurusConfig", encoding: 'utf-8', text: rendered)
  }

  def checkoutGitRepo(String repoName, String branch) {
    stage("checkout git") {
      pipeline.git branch: branch, credentialsId: gitConfig.credsId, url: "${gitConfig.baseUrl}/${repoName}.git"
    }
  }

  def runBlazemeterTaurus(taurusConfig, taurusOptions, xmlReport, csvReport) {
    stage('run testing') {
      try {
        runTaurus(taurusConfig, taurusOptions)
      }
      finally {
        publishTaurusTestResults(xmlReport, csvReport)
        publishBackendReport()
        publishGrafanaLink()
      }
    }
  }

  def runLocust(locustFile, locustClass, host, numClients, hatchRate, timeout, logFile, logLevel, reportFile) {
    stage('run testing') {
      try {
        runLocustStandalone(locustFile, locustClass, host, numClients, hatchRate, timeout, logFile, logLevel, reportFile)
      }
      finally {
        publishLocustTestResults()
        publishBackendReport()
        publishGrafanaLink()
      }
    }
  }

  def runTaurus(taurusConfig, taurusOptions) {
    pipeline.sh(script: "bzt load_test/${taurusConfig} ${taurusOptions} -o settings.artifacts-dir=${reportDir}")
  }

  def runLocustStandalone(locustFile, locustClass, host, numClients, hatchRate, timeout, logFile, logLevel = 'INFO', reportFile = 'locust-report') {
    String logFileParams = "--logfile=${reportDir}/${logFile}.log --print-stats"
    pipeline.sh 'mkdir test_artifacts'
    pipeline.sh 'locust --version'
    pipeline.sh(script: "locust -f load_test/$locustFile $locustClass " +
      "--host $host " +
      "--no-web " +
      "--clients=$numClients " +
      "--hatch-rate=$hatchRate " +
      "--run-time=${timeout} " +
      "--loglevel=$logLevel " +
      "--csv=test_artifacts/$reportFile " +
      "${logFile ? logFileParams : ''}", returnStdout: true)
  }

  def publishTaurusTestResults(reportFileXml, reportFileCsv) {
    stage("build report") {
      pipeline.sh('''\
        echo Archiving Test Artifacts:
        ls -la $(pwd)
        ls -la $(pwd)/test_artifacts
        '''.stripIndent())
      pipeline.step([$class: 'ArtifactArchiver', artifacts: "${reportFileXml}, ${reportFileCsv}", fingerprint: true])
      pipeline.step([$class: 'ArtifactArchiver', artifacts:
          "${reportDir}/*.err, " +
          "${reportDir}/*.log, " +
          "${reportDir}/*.yml, " +
          "${reportDir}/*.csv, " +
          "${reportDir}/*.py, " +
          "${reportDir}/*.jtl", fingerprint: true])
      pipeline.performanceReport(
        parsers: [[$class: 'TaurusParser', glob: reportFileXml, percentiles: '0,10,20,30,40,50,90,100', filterRegex: '']],
        relativeFailedThresholdNegative: 1.2,
        relativeFailedThresholdPositive: 1.89,
        relativeUnstableThresholdNegative: 1.8,
        relativeUnstableThresholdPositive: 1.5)
    }
  }

  def publishLocustTestResults() {
    stage("build report") {
      pipeline.sh("""\
        echo Archiving Test Artifacts:
        ls -la \$(pwd)/${reportDir}
      """.stripIndent())
      pipeline.step([$class: 'ArtifactArchiver', artifacts:
          "${reportDir}/*.xml, " +
          "${reportDir}/*.csv, " +
          "${reportDir}/*.log", fingerprint: true])
    }
  }

  def publishGrafanaLink() {
    // Installation requirements: HtmlPublisher Plugin
    // https://jenkins.io/blog/2016/07/01/html-publisher-plugin
    stage("publish grafana dashboard", grafanaUri != null && grafanaServiceName != null) {
      pipeline.step([
        $class: 'ArtifactArchiver',
        artifacts: "${reportDir}/grafana-dashboard.html, ${reportDir}/grafana-dashboard.txt"
      ])
      pipeline.publishHTML(
        [allowMissing         : false,
         alwaysLinkToLastBuild: true,
         keepAll              : true,
         reportDir            : "./${reportDir}",
         reportFiles          : 'grafana-dashboard.html',
         reportName           : 'Grafana Dashboard',
         reportTitles         : ''])
    }
  }

  def publishBackendReport() {
    stage("publish bakend report", prometheus.url != null && prometheus.service != null && prometheus.namespace != null) {
      pipeline.echo """
      Publishing Backend Report
      prometheus configurations: ${prometheus.toString()}
      """
      pipeline.step([
        $class: 'ArtifactArchiver',
        artifacts: "${reportDir}/*_chart.png, ${reportDir}/*_graph.png, ${reportDir}/*.csv, ${reportDir}/backend_report.html",
        fingerprint: true
      ])
      pipeline.publishHTML(
        [allowMissing         : false,
         alwaysLinkToLastBuild: true,
         keepAll              : true,
         reportDir            : "./${reportDir}",
         reportFiles          : "backend_report.html",
         reportName           : 'Backend Report',
         reportTitles         : ''
        ])
      // CPU vs Memory Plot
      pipeline.plot(
        csvFileName: 'plot-fb5b12c3-f858-4e70-8f6f-c0e7784b0df3.csv',
        csvSeries: [
          [
            displayTableFlag: true,
            exclusionValues: '',
            file: "${reportDir}/backend_report_cpu_memory.csv",
            inclusionFlag: 'OFF', url: '']
        ],
        group: 'prometheus',
        keepRecords: false,
        numBuilds: '', style: 'line', title: 'CPU vs Memory Graph', useDescr: true)
      // Pods vs Requests Plot
      pipeline.plot(
        csvFileName: 'plot-fb5b12c3-f859-4e70-8f6f-c0e7784b0dd3.csv',
        csvSeries: [
          [
            displayTableFlag: true,
            exclusionValues: '',
            file: "${reportDir}/backend_report_pods_requests.csv",
            inclusionFlag: 'OFF', url: '']
        ],
        group: 'prometheus',
        keepRecords: false,
        numBuilds: '', style: 'line', title: 'Pods vs Throughput Graph', useDescr: true)
    }
  }

  def installLocustJenkinsReporterLib(){
    def artifact = [
      name: 'locust-jenkins-reporter',
      tag: "$locustReportingLibVersion",
      file: "locust_jenkins_reporter-${locustReportingLibVersion}.tar.gz"
    ]
    def creds = [pipeline.usernamePassword(
      credentialsId: nexusConfig.credsId,
      usernameVariable: 'user',
      passwordVariable: 'password')]
    pipeline.withCredentials(creds){
      pipeline.echo 'Installing "locust_jenkins_reporter" library'
      pipeline.sh "wget http://${pipeline.env.user}:${pipeline.env.password}@${new URI(nexusConfig.baseUrl).host}/repository/debug-pypi-sandbox/packages/${artifact.name}/${artifact.tag}/${artifact.file}"
      pipeline.sh "pip install ${artifact.file}"
      pipeline.echo 'Reporting Libs Successfully installed.'
    }

  }

}
