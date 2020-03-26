#!groovy

def call(Map args) {
  final String branch = args.branch
  final String projectDir = args.projectDir
  final String namespace = args.namespace
  final String configPrefix = args.configPrefix
  final String serviceUrl = "http://${args.serviceName.toLowerCase()}.${namespace}"
  def podEnvVars = [
    envVar(key: 'AH_FUNTEST_SUT_URL', value: serviceUrl),
    secretEnvVar(key: 'NUGET_CONFIG', secretName: 'infra-secretconfigs', secretKey: 'nuget.config')
    ]
  podTemplate_migration(JOB_BASE_NAME, BUILD_ID, namespace, AH_CONFIGURATION_PATH, configPrefix, podEnvVars) {
    node("slvm_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
      echo("Run tests against ${serviceUrl}")
      checkoutSCM(branch, GLOBAL_GITSCM_CREDSID, GITSCM_REPO_URL)
      if (args.beforeTestRun) {
        args.beforeTestRun()
      }
      dotnetTest(projectDir: projectDir)
      if (args.afterTestRun) {
        args.afterTestRun()
      }
    }
  }
}
