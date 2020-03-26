package com.e4d.job

import com.e4d.build.TemplateEngine
import com.e4d.git.GitConfig
import com.e4d.k8s.K8sClient
import com.e4d.k8s.K8sConfig
import com.e4d.template.MapMerger

class PerformanceTestClusterJob extends PipelineJob {

  GitConfig gitConfig = new GitConfig()
  K8sConfig k8sConfig = new K8sConfig()
  K8sClient k8sClient
  String framework = 'locust'    // Supported frameworks: taurus, locust
  String repoName                // Repository name for specific microservice
  String branch = 'develop'      // Specific branch name
  String context                 // dev, experimental or prod
  String namespace = 'locust'    // default namespace
  Integer deploymentTimeout = 60 // Deployment timeout - seconds
  String bztConfigsPath = 'configs'
  String testsDirectory = "load_test"
  String grafanaUri
  String grafanaServiceName
  String grafanaServiceNamespace = 'default'
  String kibanaUri
  String locustFile
  String locustClass
  String host
  Integer numClients
  Integer hatchRate
  String runTime
  String reportFileName = 'locust-report'
  String xmlReport = 'locust-report.xml'
  String masterTemplate = 'load_test/deploy/kubernetes/locust_master_template.yml'
  String slavesTemplate = 'load_test/deploy/kubernetes/locust_workers_template_hpa.yml'
  String taurusTemplate = 'load_test/deploy/kubernetes/locust_taurus_job_template.yml'

  Map cluster = [:]
  Map taurus  = [:]
  final TemplateEngine templateEngine

  PerformanceTestClusterJob(Object pipeline) {
    super(pipeline)
    k8sClient = new K8sClient(pipeline)
    k8sConfig.with(DefaultValues.k8s)
    gitConfig.with(DefaultValues.git)
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

  def run() {
    checkoutGitRepo(repoName, branch)
    mountTestCode()

    if (framework == 'taurus') {
      runTaurusDeployment()
    } else if (framework == 'locust') {
      runLocustClusterDeployment()
    } else {
      return pipeline.error("Framework: $framework - not supported!!!")
    }
    cleanUpNamespace()
  }

  def checkoutGitRepo(String repoName, String branch){
    stage("checkout git"){
      pipeline.git branch: branch, credentialsId: gitConfig.credsId, url: "${gitConfig.baseUrl}/${repoName}.git"
    }
  }

  def mountTestCode(){
    stage('mount test code'){
      pipeline.sh """
      kubectl create configmap load-test --from-file $testsDirectory --namespace=$namespace
      kubectl create configmap configs --from-file $testsDirectory/$bztConfigsPath/ --namespace=$namespace
      """
    }
  }

  def initScriptEnvVars(pipeline){
    pipeline.env.MASTER_TEMPLATE = masterTemplate
    pipeline.env.SLAVES_TEMPLATE = slavesTemplate
    pipeline.env.TAURUS_TEMPLATE = taurusTemplate
    pipeline.env.DEPLOY_TIMEOUT = deploymentTimeout
  }

  def prepareClusterData(){
    new MapMerger().merge(cluster, [
      namespace: namespace,
      grafanaUri: grafanaUri,
      grafanaServiceName: grafanaServiceName,
      grafanaServiceNamespace: grafanaServiceNamespace,
      kibanaUri: kibanaUri,
      expectSlaves: cluster.expectSlaves ?: 2,
      host: host ?: cluster.host,
      locustFile: locustFile ?: cluster.locustFile,
      locustClass: locustClass ?: cluster.locustClass ?: "''",
      numClients: numClients ?: cluster.numClients,
      hatchRate: hatchRate ?: cluster.hatchRate,
      runTime: runTime ?: cluster.runTime,
      reportFileName: reportFileName ?: cluster.reportFileName,
      logFileName: 'locust-master.log',
      service: [
        requests: [
          cpu:  0.5,
          memory: "'400Mi'"
        ],
        limits: [
          cpu:  0.5,
          memory: "'400Mi'"
      ]]
    ])
    def masterContent = pipeline.readFile(file: masterTemplate, encoding: 'utf-8').toString()
    def slavesContent = pipeline.readFile(file: slavesTemplate, encoding: 'utf-8').toString()
    def renderedMaster = templateEngine.render(masterContent, cluster)
    def renderedSlaves = templateEngine.render(slavesContent, cluster)
    pipeline.echo(renderedMaster)
    pipeline.echo(renderedSlaves)
    pipeline.writeFile(file: masterTemplate, encoding: 'utf-8', text: renderedMaster)
    pipeline.writeFile(file: slavesTemplate, encoding: 'utf-8', text: renderedSlaves)
  }

  def runLocustClusterDeployment(){
    stage('run testing'){
      prepareClusterData()
      initScriptEnvVars(pipeline)
      pipeline.catchError {
        pipeline.sh(pipeline.libraryResource('com/e4d/locust/k8s-run-locust-cluster.sh'))
      }
      publishLocustTestResults()
      publishGrafanaDashboard("test_artifacts")
    }
  }

  def prepareTaurusData(){
    new MapMerger().merge(taurus, [
      namespace: namespace,
      grafanaUri: grafanaUri,
      grafanaServiceName: grafanaServiceName,
      grafanaServiceNamespace: grafanaServiceNamespace,
      kibanaUri: kibanaUri,
      host: host ?: taurus.host,
      taurusConfig: taurus.taurusConfig,
      taurusOpts: taurus.taurusOpts ?: '-report'
    ])
    def content = pipeline.readFile(file: taurusTemplate, encoding: 'utf-8').toString()
    def rendered = templateEngine.render(content, taurus)
    pipeline.echo(rendered)
    pipeline.writeFile(file: taurusTemplate, encoding: 'utf-8', text: rendered)
  }

  def runTaurusDeployment(){
    prepareTaurusData()
    initScriptEnvVars(pipeline)
    stage('run testing'){
      pipeline.catchError {
        pipeline.sh(pipeline.libraryResource('com/e4d/locust/k8s-run-locust-blazemeter.sh'))
      }
      publishTaurusTestResults(xmlReport)
      publishGrafanaDashboard("reports")
    }
  }

  def publishTaurusTestResults(reportFileXml) {
    stage("build report"){
      pipeline.sh("""\
        echo Archiving Test Artifacts:
        ls -la \$(pwd)
        ls -la ./reports
        ls -la ./artifacts
        """.stripIndent())
      pipeline.performanceReport(
        parsers: [[$class: 'TaurusParser', glob: "reports/${reportFileXml}", percentiles: '0,10,20,30,40,50,90,100', filterRegex: '' ]],
        relativeFailedThresholdNegative: 1.2,
        relativeFailedThresholdPositive: 1.89,
        relativeUnstableThresholdNegative: 1.8,
        relativeUnstableThresholdPositive: 1.5)
      pipeline.step([
        $class: 'ArtifactArchiver',
        artifacts: "reports/*.xml, reports/*.csv, reports/*.html, reports/*.txt, reports/*.log"
      ])
      pipeline.step([
        $class: 'ArtifactArchiver',
        artifacts: "artifacts/*.xml, artifacts/*.csv, artifacts/*.html, artifacts/*.txt, artifacts/*.log"
      ])
    }
  }

  def publishLocustTestResults(){
    stage("build report"){
      pipeline.sh """
        ls -la \$(pwd)
        ls -la ${testsDirectory}
        echo "Locust Output:"
        ls -la test_artifacts
        """
      pipeline.step([
        $class: 'ArtifactArchiver',
        artifacts: "test_artifacts/*.xml, test_artifacts/*.csv, test_artifacts/*.html, test_artifacts/*.txt, test_artifacts/*.log"
      ])
    }
  }

  def publishGrafanaDashboard(artifactsDir){
    // Installation requirements: HtmlPublisher Plugin
    // https://jenkins.io/blog/2016/07/01/html-publisher-plugin
    stage("publish grafana dashboard", grafanaUri != null && grafanaServiceName != null){
      pipeline.publishHTML(
        [allowMissing         : false,
         alwaysLinkToLastBuild: false,
         keepAll              : true,
         reportDir            : "./${artifactsDir}",
         reportFiles          : 'grafana-dashboard.html',
         reportName           : 'Grafana Dashboard',
         reportTitles         : ''])
    }
  }

  def cleanUpNamespace(){
    stage('cleanup namespace'){
      pipeline.sh """
      kubectl delete configmap load-test --namespace=$namespace
      kubectl delete configmap configs --namespace=$namespace
      """
      if (cluster) {
        pipeline.sh "kubectl delete deployment locust-worker --namespace=$namespace\n" +
          "kubectl delete deployment locust-master --namespace=$namespace\n" +
          "kubectl delete service locust-master --namespace=$namespace\n" +
          "${if (slavesTemplate.contains('hpa')) { "kubectl delete hpa locust-worker-hpa --namespace=$namespace" } else { ' ' }}"
      } else if (taurus){
        pipeline.sh "kubectl delete jobs locust-taurus-job --namespace=$namespace"
      }
    }
  }

}

