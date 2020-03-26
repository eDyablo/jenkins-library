#!groovy

import com.e4d.config.*
import com.e4d.build.*
import com.e4d.dotnet.*
import com.e4d.pip.*
import com.e4d.k8s.*
import com.e4d.service.*

def call(
  BRANCH_NAME,
  OVERRIDEN_SECRETS,
  OVERRIDEN_CONFIGS,
  EnVars = [],
  body
) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  OVERRIDEN_CONFIGS = OVERRIDEN_CONFIGS.trim()
  OVERRIDEN_CONFIGS -= '--override configuration.json='
  if (OVERRIDEN_CONFIGS.startsWith("'"))
    OVERRIDEN_CONFIGS = OVERRIDEN_CONFIGS[1..-1]
  if (OVERRIDEN_CONFIGS.endsWith("'"))
    OVERRIDEN_CONFIGS = OVERRIDEN_CONFIGS[0..-2]
  if (OVERRIDEN_CONFIGS.startsWith('{'))
    OVERRIDEN_CONFIGS = '"configuration.json": ' + OVERRIDEN_CONFIGS

  OVERRIDEN_SECRETS = OVERRIDEN_SECRETS.trim()
  if (OVERRIDEN_SECRETS.startsWith("'"))
    OVERRIDEN_SECRETS = OVERRIDEN_SECRETS[1..-1]
  if (OVERRIDEN_SECRETS.endsWith("'"))
    OVERRIDEN_SECRETS = OVERRIDEN_SECRETS[0..-2]
  if (OVERRIDEN_SECRETS.startsWith('{'))
    OVERRIDEN_SECRETS = '"secret.json": ' + OVERRIDEN_SECRETS

  boolean pullRequestBranch = BRANCH_NAME ==~ /origin\/pr\/.*/
  boolean publishRequested = params.ISPUBLISH == true && pullRequestBranch == false
  boolean canPublish = env.JOB_NAME ==~ /e4d\/PostMergeJobs\/.+/
  boolean requireIntegrationTests = (params.SKIP_INTEGRATION_TESTS ?: false) == false
  boolean requireFunctionalTests = (params.SKIP_FUNCTIONAL_TESTS ?: true) == false
  boolean requireK8sConfig = requireIntegrationTests

  String unitTestProjectPattern = /.*\.([Tt]est|[Uu]nit[Tt]est)(s?).*\.csproj/

  def dotnetConfig = new DotnetConfig()
  dotnetConfig.nugetConfigRef = 'infra-secretconfigs : nuget.config'

  def k8sConfig = new K8sConfig()
  k8sConfig.configRef = 'infra-secretconfigs : kube.config'

  def pipConfig = new PipConfig()
  pipConfig.configRef = 'infra-secretconfigs : pip.config'

  def dotnetClient = new DotnetClient(this, dotnetConfig)
  def k8sClient = new K8sClient(this)
  def configClient = new ConfigClient(this, k8sConfig, pipConfig)

  def templateEngine = new TemplateEngine(this)

  def serviceConfig = new CSharpServiceConfig()
  serviceConfig.service = SERVICE_NAME
  serviceConfig.configmapPatch = templateEngine.render(OVERRIDEN_CONFIGS)
  serviceConfig.secretPatch = templateEngine.render(OVERRIDEN_SECRETS)

  def jobId = NameUtils.shortTimelyUniqueName(env.JOB_NAME + env.BUILD_ID)

  withCredentials([
    usernamePassword(credentialsId: GLOBAL_GITSCM_CREDSID,
    usernameVariable: 'GIT_USERNAME',
    passwordVariable: 'GIT_PASSWORD')
  ]) {
    def VERSION = "$E4D_BUILD_ID_BASE.$BUILD_ID"
    if (pullRequestBranch) {
      VERSION = "$E4D_BUILD_ID_BASE.$BUILD_ID-pr"
    }

    def JENKINS_NAMESPACE       = "jenkins"
    def automationNamespace    = "ci-$SERVICE_NAME-$jobId".toLowerCase()
    def SERVICE_NAME_LOWER      = SERVICE_NAME.toLowerCase()
    def IS_DEBUG                = Boolean.toString(BUILD_CONFIGURATION.toLowerCase() == 'debug')
    SERVICE_DEFAULT_SECRET_NAME = SERVICE_SECRET_NAME ?: "$SERVICE_NAME-secret".toLowerCase()

    def envVars = [
      envVar(key: 'AH_SCM_TAG',                         value: GITSCM_TAG),
      envVar(key: 'AH_SERVICE_NAME',                    value: SERVICE_NAME),
      envVar(key: 'AH_CLUSTER',                         value: AH_CLUSTER),
      envVar(key: 'AH_NAMESPACE',                       value: AH_NAMESPACE),
      envVar(key: 'AH_CONFIGURATION_PATH',              value: AH_CONFIGURATION_PATH),
      envVar(key: 'AH_CONFIGURATION_DEBUG',             value: AH_CONFIGURATION_DEBUG),
      
      envVar(key: 'SERVICE_NAME_LOWER',                 value: SERVICE_NAME_LOWER),
      envVar(key: 'SERVICE_IMAGE',                      value: SERVICE_IMAGE),
      envVar(key: 'PROJECT_PATH',                       value: SERVICE_PROJECT_PATH),
      envVar(key: 'BUILD_CONFIGURATION',                value: BUILD_CONFIGURATION),
      envVar(key: 'GITSCM_REPO_URL',                    value: GITSCM_REPO_URL),
      envVar(key: 'BRANCH_NAME',                        value: BRANCH_NAME),
      envVar(key: 'E4D_BUILD_ID_BASE',               value: E4D_BUILD_ID_BASE),      
      envVar(key: 'GIT_USERNAME',                       value: GIT_USERNAME),
      envVar(key: 'GIT_PASSWORD',                       value: GIT_PASSWORD),
      envVar(key: 'VERSION',                            value: VERSION),
      envVar(key: 'BUILD_ID',                           value: BUILD_ID),
      envVar(key: 'ISPUBLISH',                          value: ISPUBLISH),
      envVar(key: 'JENKINS_NAMESPACE',                  value: JENKINS_NAMESPACE),
      envVar(key: 'PREDEPLOYMENT_SCRIPT_PATH',          value: PREDEPLOYMENT_SCRIPT_PATH),
      envVar(key: 'CONFIGS_ROOT',                       value: CONFIGS_ROOT),
      envVar(key: 'JOB_BASE_NAME',                      value: JOB_BASE_NAME),

      envVar(key: 'NEXUS_REPO_URL',                     value: GLOBAL_NEXUS_REPO_URL),
      envVar(key: 'NEXUS_PIP_REPO_URL',                 value: GLOBAL_NEXUS_PIP_REPO_URL),
      envVar(key: 'GITSCM_CREDSID',                     value: GLOBAL_GITSCM_CREDSID),

      secretEnvVar(key: 'NEXUS_APIKEY',                 secretName: 'infra-credentials',   secretKey: 'nexus.apikey'),
      secretEnvVar(key: 'NEXUS_USER',                   secretName: 'infra-credentials',   secretKey: 'nexus.username'),
      secretEnvVar(key: 'NEXUS_PASSWORD',               secretName: 'infra-credentials',   secretKey: 'nexus.password')
    ] +
    dotnetConfig.defineEnvVars(this) +
    pipConfig.defineEnvVars(this) +
    k8sConfig.defineEnvVars(this) +
    EnVars
 
    try {
      printParameters()

      podTemplate_basic_singleSlave(JENKINS_NAMESPACE, envVars) {
        node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
          stage('Pull') {
            checkoutSCM(BRANCH_NAME, GLOBAL_GITSCM_CREDSID, GITSCM_REPO_URL)
          }
          if (!config.skipUnitTests) {
            stage('Run Unit Tests') {
              config.preUnitTestHook?.call()
              container('jenkins-slave') {
                def testResults = dotnetClient.test('.',
                    configuration: BUILD_CONFIGURATION,
                    includeProjects: unitTestProjectPattern)
                publishTestReport(testResults)
              }
              config.postUnitTestHook?.call()
            }
          }
          if (requireK8sConfig) {
            stage('Apply K8S Configs') {
              config.preApplyK8SConfigsHook?.call()
              container('jenkins-slave') {
                k8sClient.context = AH_CLUSTER
                configClient.setup()

                configClient.copySecrets(
                    source: JENKINS_NAMESPACE,
                    destination: automationNamespace,
                    names: [
                      'infra-secretconfigs',
                      'infra-urls',
                      'infra-credentials',
                      'regsecret',
                      SERVICE_DEFAULT_SECRET_NAME
                    ])

                configClient.createConfigmaps(
                    configDir: CONFIGS_ROOT,
                    env: 'testing',
                    prefix: serviceConfig.service.name.toLowerCase(),
                    namespace: automationNamespace,
                    patch: serviceConfig.configmapPatch)

                configClient.patchSecret(
                    namespace: automationNamespace,
                    name: SERVICE_DEFAULT_SECRET_NAME,
                    patch: serviceConfig.secretPatch)
              }
              config.postApplyK8SConfigsHook?.call()
            }
          }
        }
      }

      if (requireIntegrationTests) {
        stage('Run Integration Tests') {
          microserviceIntegrationTesting(
            branch: BRANCH_NAME,
            namespace: automationNamespace,
            configPrefix: serviceConfig.service.name.toLowerCase(),
            excludeProjects: config.excludeProjects,
            beforeTestRun: config.preIntegrationTestsRunHook,
            afterTestRun: config.postIntegrationTestsRunHook
          )
        }
      }

      def publishedImage = null

      if (publishRequested) {
        if (canPublish == false) {
          throw new Exception("Can publish only by a post merge job.")
        }
        podTemplate_basic(JENKINS_NAMESPACE, envVars + [envVar(key: 'VERSION', value: VERSION)]) {
          node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
            checkoutSCM("${BRANCH_NAME}", "${GLOBAL_GITSCM_CREDSID}", "${GITSCM_REPO_URL}")
            config.preBuildHook?.call()
            if (!config.skipDockerBuildAndPublish) {
              stage('Build docker') {
                dotnetPublish(
                  projectDir: SERVICE_PROJECT_PATH,
                  buildConfig: BUILD_CONFIGURATION
                )
              }
            }
            config.postBuildHook?.call()
            if (!config.skipDockerBuildAndPublish) {
              stage('Push to docker registry') {
                publishedImage = dockerPublish(
                  projectDir: SERVICE_PROJECT_PATH,
                  serviceImage: SERVICE_IMAGE,
                  version: VERSION,
                  beforePublish: config.preDockerPushHook,
                  afterPublish: config.postDockerPushHook
                )
              }
            }
            else {
              config.preDockerPushHook?.call()
              config.postDockerPushHook?.call()
            }
            if (!config.skipDockerBuildAndPublish) {
              if (JOB_NAME =~ /PostMergeJobs/) {
                stage('Tag SCM') {
                  container('dotnetcore-sdk') {              
                    sh '''
                      REPLACE_PATTERN="s/https:\\/\\//https:\\/\\/"${GIT_USERNAME}":"${GIT_PASSWORD}"@/g"
                      PUSH_URL=$(echo $GITSCM_REPO_URL | sed $REPLACE_PATTERN)
                      REPLACE_PATTERN="https://${GIT_USERNAME}:${GIT_PASSWORD}@"
                      git config --global user.email "ci@e4d.com"
                      git config --global user.name "Jenkins"
                      git tag -a $VERSION -m "Jenkins: Microservice image version [ci skip] ."
                      git push $PUSH_URL --tags
                    '''
                  }
                }
              }
              else {
                echo "Tagging SCM skipped: job is not in PostMergeJobs folder"  
              }  
            }
          }
        }
      }

      if (requireFunctionalTests && publishedImage) {
        def kubeConfig = k8s.defSecretEnvVar(key: 'KUBE_CONFIG', secretName: 'infra-secretconfigs', secretKey: 'kube.config')
        def nugetConfig = k8s.defSecretEnvVar(key: 'NUGET_CONFIG', secretName: 'infra-secretconfigs', secretKey: 'nuget.config')
        stage("Configure for functional testing") {
          configureService(
            serviceName: SERVICE_NAME,
            branch: BRANCH_NAME,
            gitTag: VERSION,
            sourceNamespace: JENKINS_NAMESPACE,
            targetNamespace: automationNamespace,
            pipRepoUrl: GLOBAL_NEXUS_PIP_REPO_URL,
            kubeContext: AH_CLUSTER,
            kubeConfig: kubeConfig,
            nexusUser: k8s.defSecretEnvVar(key: 'NEXUS_USER', secretName: 'infra-credentials', secretKey: 'nexus.username'),
            nexusPassword: k8s.defSecretEnvVar(key: 'NEXUS_PASSWORD', secretName: 'infra-credentials', secretKey: 'nexus.password'),
            configDir: CONFIGS_ROOT,
            configEnv: 'funtest',
            secretOverrides: OVERRIDEN_SECRETS
          )
        }
        stage("Deploy for functional testing") {
          deployService(
            isDebug: "${BUILD_CONFIGURATION}".toLowerCase() == 'debug',
            serviceName: SERVICE_NAME,
            serviceImage: publishedImage.name,
            branch: BRANCH_NAME,
            gitTag: VERSION,
            templatePath: SERVICE_YAML_TEMPLATE_PATH,
            podNamespace: JENKINS_NAMESPACE,
            serviceNamespace: automationNamespace,
            kubeContext: AH_CLUSTER,
            kubeConfig: kubeConfig,
            configPrefix: serviceConfig.service.name.toLowerCase(),
            predeploymentScriptPath: PREDEPLOYMENT_SCRIPT_PATH,
            nugetConfig: nugetConfig
          )
        }
        stage("Run functional tests") {
          microserviceFunctionalTesting(
            serviceName: SERVICE_NAME,
            branch: BRANCH_NAME,
            projectDir: SERVICE_PROJECT_PATH + '.FunTests',
            namespace: automationNamespace,
            configPrefix: serviceConfig.service.name.toLowerCase(),
            kubeConfig: kubeConfig
          )
        }
      }
    }
    catch (all) {
      error(all.message)
    }
    finally {
      podTemplate_basic(JENKINS_NAMESPACE, envVars) {
        node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
          config.preCleanUpHook?.call()

          if (!config["skipCleanUp"]) {
            stage('CleanUp') {
              container('kube') {
                k8sClient.context = AH_CLUSTER
                k8sClient.deleteResource('namespace', automationNamespace,
                    ignoreNotFound: true)
              }
            }
          }

          config.postCleanUpHook?.call()
        }
      }
    }
  }
}
