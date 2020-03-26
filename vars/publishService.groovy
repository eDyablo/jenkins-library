#!groovy

def call(Map args) {
  final String branch = args.branch
  final String projectDir = args.projectDir
  final String version = args.version
  final String namespace = args.namespace ?: 'jenkins'
  final String jobName = args.jobName ?: JOB_BASE_NAME
  final String buildId = args.buildId ?: BUILD_ID
  final String repoUrl = args.repoUrl ?: GITSCM_REPO_URL
  final String repoCredentialsId = args.repoCredentialsId ?: GLOBAL_GITSCM_CREDSID
  final String buildConfig = args.buildConfig ?: BUILD_CONFIGURATION
  final String nodeName = "slv_${jobName}_${buildId}".toLowerCase()

  def podEnvVars = [
    secretEnvVar(key: 'NUGET_CONFIG', secretName: 'infra-secretconfigs', secretKey: 'nuget.config'),
    secretEnvVar(key: 'NEXUS_APIKEY', secretName: 'infra-credentials',   secretKey: 'nexus.apikey'),
    secretEnvVar(key: 'NEXUS_USER', secretName: 'infra-credentials',   secretKey: 'nexus.username'),
    secretEnvVar(key: 'NEXUS_PASSWORD', secretName: 'infra-credentials',   secretKey: 'nexus.password')
  ]

  podTemplate_basic(namespace, podEnvVars) {
    node(nodeName) {
      checkoutSCM(branch, repoCredentialsId, repoUrl)
      dotnetPublish(projectDir: projectDir, buildConfig: buildConfig)
      dockerPublish(projectDir: projectDir, version: version)
    }
  }
}
