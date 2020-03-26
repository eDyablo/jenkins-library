import com.e4d.dotnet.*

def call(Map args) {
  def branch = args.branch
  def namespace = args.namespace
  def configPrefix = args.configPrefix

  def dotnetConfig = new DotnetConfig()
  dotnetConfig.nugetConfigRef = 'infra-secretconfigs : nuget.config'

  String databaseMigrationProjectPattern = /.*\.[Dd]ata\.[Mm]igrations\.csproj/
  String integrationTestProjectPattern = /.*\.[Ii]ntegration[Tt]est(s?).*\.csproj/

  def podEnvVars = [
    envVar(key: 'AH_SERVICE_NAME', value: SERVICE_NAME),
    envVar(key: 'AH_SCM_TAG', value: GITSCM_TAG),
    envVar(key: 'AH_CONFIGURATION_PATH', value: AH_CONFIGURATION_PATH),
    envVar(key: 'AH_CONFIGURATION_DEBUG', value: AH_CONFIGURATION_DEBUG)
  ] +
  dotnetConfig.defineEnvVars(this)

  podTemplate_migration_singleSlave(JOB_BASE_NAME, BUILD_ID, namespace,
      AH_CONFIGURATION_PATH, configPrefix, podEnvVars) {
    node("slvm_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
      checkoutSCM(branch, GLOBAL_GITSCM_CREDSID, GITSCM_REPO_URL)

      args.beforeTestRun?.call()

      container('jenkins-slave') {
        def dotnetClient = new DotnetClient(this, dotnetConfig)

        dotnetClient.runTool(tool: 'ef', command: 'database',
            arguments: ['update'],
            baseDir: '.',
            includeProjects: databaseMigrationProjectPattern)

        dotnetClient.test('.',
            configuration: BUILD_CONFIGURATION,
            includeProjects: integrationTestProjectPattern,
            excludeProjects: args.excludeProjects)
      }

      args.afterTestRun?.call()
    }
  }
}
