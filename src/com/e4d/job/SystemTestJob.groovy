package com.e4d.job

import com.e4d.build.TemplateEngine
import com.e4d.git.GitConfig
import com.e4d.k8s.K8sClient
import com.e4d.k8s.K8sConfig
import com.e4d.nexus.NexusConfig
import com.e4d.nuget.NugetConfig
import com.e4d.pip.PipConfig
import com.e4d.template.MapMerger

class SystemTestJob extends PipelineJob {

  GitConfig gitConfig = new GitConfig()
  NexusConfig nexusConfig = new NexusConfig()
  NugetConfig nugetConfig = new NugetConfig()
  PipConfig pipConfig = new PipConfig()
  K8sConfig k8sConfig = new K8sConfig()
  K8sClient k8sClient
  String repoName = "ah-system-test" // default repository with system tests
  String branch = "develop"          // default branch
  String context                     // Cluster environment to tun System Tests
  String environment
  String baseUrl
  String testFilter = "TestCategory=System"
  String verbosity = "normal"        // [ quiet, minimal, normal, detailed ]
  String testDir = "scr/SystemTests/SystemTests"
  String defaultProject = "SystemTests.csproj"
  String jiraLinkPattern = "https://e4d.atlassian.net/browse/{}"
  String settingsFile = "test.runsettings"
  boolean disableParallelization = false
  String maxCpuCount = '1'
  int numberOfTestWorkers = 3
  String rootNugetConf = "/root/.nuget/NuGet/NuGet.Config"
  String dotnetVersion = "3.1"
  def testVars = [:]
  def runSettings = [:]
  final TemplateEngine templateEngine


  SystemTestJob(pipeline) {
    super(pipeline)
    k8sClient = new K8sClient(pipeline)
    k8sConfig.with(DefaultValues.k8s)
    nexusConfig.with(DefaultValues.nexus)
    nugetConfig.with(DefaultValues.nuget)
    pipConfig.with(DefaultValues.pip)
    gitConfig.with(DefaultValues.git)
    gitConfig.branch = branch
    templateEngine = new TemplateEngine(pipeline)
  }

  def defineEnvVars() {
    super.defineEnvVars() +
      k8sConfig.defineEnvVars(pipeline).findAll() +
      pipConfig.defineEnvVars(pipeline).findAll() +
      nugetConfig.defineEnvVars(pipeline).findAll()
  }

  def defineVolumes() {
    super.defineVolumes() +
      k8sConfig.defineVolumes(pipeline) +
      pipConfig.defineVolumes(pipeline).findAll() +
      nugetConfig.defineVolumes(pipeline).findAll()
  }

  void initializeJob() {
    k8sClient.configPath = k8sConfig.configPath
    k8sClient.context = context ?: 'default'
  }

  def run() {
    pipeline.sh "cp -r ${pipeline.env.NUGET_CONFIG} ${rootNugetConf}"
    initEnvVars(pipeline)
    checkoutGitRepo(repoName, branch)
    prepareTestRunSettingsFiles()
    dotnetBuild()
    printTestSettings()
    test()
  }

  def checkoutGitRepo(String repoName, String branch) {
    stage("checkout git") {
      pipeline.git branch: branch, credentialsId: gitConfig.credsId, url: "${gitConfig.baseUrl}/${repoName}.git"
    }
  }

  def renderTestSettingsToDotnetFile(String fileName, String targetFile){
    // Copy File from pipeline
    def runSettingsFile = pipeline.libraryResource "com/e4d/dotnet/nunit/${fileName}"
    pipeline.writeFile file: "${targetFile}", encoding: 'utf-8', text: runSettingsFile
    // Render Template file
    def content = pipeline.readFile(file: "${targetFile}", encoding: 'utf-8').toString()
    def rendered = templateEngine.render(content, runSettings)
    pipeline.echo(rendered)
    pipeline.writeFile(file: "${targetFile}", encoding: 'utf-8', text: rendered)
  }

  def prepareTestRunSettingsFiles(){
    pipeline.echo "Creating Nunit RunSettings."
    new MapMerger().merge(runSettings, [
      maxCpuCount: "${maxCpuCount}",
      disableParallelization: "${disableParallelization}",
      numberOfTestWorkers: "${numberOfTestWorkers}"
    ])
    renderTestSettingsToDotnetFile(settingsFile, "${testDir}/${settingsFile}")
    renderTestSettingsToDotnetFile("AssemblyInfo.cs", "${testDir}/Properties/AssemblyInfo.cs")

  }

  def prepareTestVars() {
    new MapMerger().merge(testVars, [
      COMPONENT_NAME: testVars.COMPONENT_NAME ?: repoName,
      COMPONENT_VERSION: testVars.COMPONENT_VERSION ?: "unknown",
      STAND: testVars.STAND ?: "context: ${k8sClient.context}; branch: ${branch}"
    ])
  }

  def initEnvVars(pipeline) {
    prepareTestVars()
    if (baseUrl){ pipeline.env.BASE_URL = baseUrl }
    if (environment) { pipeline.env.ENV_NAME = environment }
    testVars.each { key, value ->
      pipeline.env."${key}" = "${value}"
    }
  }

  def printTestSettings(){
    def appsettings = pipeline.readJSON(file: "${testDir}/appsettings.json")
    pipeline.echo("Running System Tests with Settings:")
    pipeline.echo("Environment: ${environment}")
    pipeline.echo(appsettings['AppSettings']["${environment}"].toString())
  }

  def dotnetBuild() {
    pipeline.stage('build') {
      pipeline.sh '''
      dotnet --list-sdks
      dotnet --version
      dotnet build
      '''
    }
  }

  def dotnetTest() {
    pipeline.stage('test') {
      pipeline.sh """
      dotnet test ${testDir}/${defaultProject} \\
      --filter \"${testFilter}\" \\
      --logger:"console;verbosity=${verbosity}" \\
      --settings:"${testDir}/${settingsFile}" \\
      --no-build
      """
    }
  }

  def test() {
    try {
      dotnetTest()
    } finally {
      publishNUnitTestResults()
    }
  }

  def publishNUnitTestResults() {
    stage("test report") {
      allureReport()
      attachArtifacts()
    }
  }

  def attachArtifacts() {
    pipeline.step([
      $class: 'ArtifactArchiver',
      artifacts:
        "${testDir}/bin/Debug/netcoreapp${dotnetVersion}/TestExecution.log, " +
        "${testDir}/bin/Debug/netcoreapp${dotnetVersion}/environment.properties, " +
        "${testDir}/bin/Debug/netcoreapp${dotnetVersion}/categories.json, " +
        "${testDir}/${settingsFile}, " +
        "${testDir}/Properties/AssemblyInfo.cs"

    ])
  }

  def allureReport() {
    pipeline.allure(jdk: '',
      properties: [
        [key: 'allure.issues.tracker.pattern', value: "$jiraLinkPattern"],
        [key: 'allure.title', value: 'System Tests']
      ], report: 'allure-report', results: [ [path: "${testDir}/bin/Debug/netcoreapp${dotnetVersion}/allure-results"]])
  }

}

