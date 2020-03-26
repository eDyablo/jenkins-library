#!groovy

import com.e4d.config.*
import com.e4d.build.*
import com.e4d.pip.*
import com.e4d.k8s.*
import com.e4d.service.*

def call(EnVars = [], body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def k8sConfig = new K8sConfig()
  k8sConfig.configRef = 'infra-secretconfigs : kube.config'

  def pipConfig = new PipConfig()
  pipConfig.configRef = 'infra-secretconfigs : pip.config'

  def k8sClient = new K8sClient(this)
  def configClient = new ConfigClient(this, k8sConfig, pipConfig)

  def templateEngine = new TemplateEngine(this)

  def serviceConfig = new CSharpServiceConfig()
  serviceConfig.service = SERVICE_NAME
  serviceConfig.configmapPatch = templateEngine.render(
      env.DEPLOY_CONFIG_PATCH ?: '')
  serviceConfig.secretPatch = templateEngine.render(
      env.DEPLOY_SECRET_PATCH ?: '')

  def jobId = NameUtils.shortTimelyUniqueName(env.JOB_NAME + env.BUILD_ID)

  def jenkinsNamespace = "jenkins"
  def automationNamespace = "ci-$SERVICE_NAME-$jobId".toLowerCase()
  def serviceNameLower = SERVICE_NAME.toLowerCase()
  def configPrefix = "${serviceConfig.service.name}-$GITSCM_TAG".toLowerCase()
  def sourceSecret = SERVICE_SECRET_NAME ?: "$SERVICE_NAME-secret".toLowerCase()
  def targetSecret = "$configPrefix-secret"
    
  def envVars = [
      envVar(key: 'AH_SCM_TAG',                         value: GITSCM_TAG),
      envVar(key: 'AH_SERVICE_NAME',                    value: serviceNameLower),
      envVar(key: 'AH_CLUSTER',                         value: AH_CLUSTER),
      envVar(key: 'AH_NAMESPACE',                       value: AH_NAMESPACE),
      envVar(key: 'AH_CONFIGURATION_PATH',              value: AH_CONFIGURATION_PATH),
      envVar(key: 'AH_CONFIGURATION_DEBUG',             value: AH_CONFIGURATION_DEBUG),
      envVar(key: 'AH_CONFIGURATION_ENV',               value: params.AH_CONFIGURATION_ENV ?: ''),
      envVar(key: 'SERVICE_NAME',                       value: SERVICE_NAME),
      envVar(key: 'SERVICE_IMAGE',                      value: SERVICE_IMAGE),
      envVar(key: 'SERVICE_YAML_TEMPLATE_PATH',         value: SERVICE_YAML_TEMPLATE_PATH),
      envVar(key: 'GITSCM_TAG',                         value: GITSCM_TAG),
      envVar(key: 'GITSCM_TAG_NO_DOTS',                 value: GITSCM_TAG.replaceAll("\\.", "-")),
      envVar(key: 'JOB_BASE_NAME',                      value: JOB_BASE_NAME),
      envVar(key: 'BUILD_ID',                           value: BUILD_ID),
      envVar(key: 'GITSCM_REPO_URL',                    value: GITSCM_REPO_URL),
      envVar(key: 'PREDEPLOYMENT_SCRIPT_PATH',          value: PREDEPLOYMENT_SCRIPT_PATH),
      envVar(key: 'MINIMUM_REPLICAS_COUNT',             value: MINIMUM_REPLICAS_COUNT),
      envVar(key: 'MAXIMUM_REPLICAS_COUNT',             value: MAXIMUM_REPLICAS_COUNT),
      envVar(key: 'GITSCM_CREDSID',                     value: GLOBAL_GITSCM_CREDSID),
      envVar(key: 'NEXUS_DOCKER_REGISTRY_URL',          value: GLOBAL_NEXUS_DOCKER_REGISTRY_URL),
      envVar(key: 'NEXUS_PIP_REPO_URL',                 value: GLOBAL_NEXUS_PIP_REPO_URL),
      secretEnvVar(key: 'NUGET_CONFIG',                 secretName: 'infra-secretconfigs', secretKey: 'nuget.config'),
      secretEnvVar(key: 'PYPI_CONFIG',                  secretName: 'infra-secretconfigs', secretKey: 'pypi.config'),
      secretEnvVar(key: 'NEXUS_APIKEY',                 secretName: 'infra-credentials',   secretKey: 'nexus.apikey'),
      secretEnvVar(key: 'NEXUS_USER',                   secretName: 'infra-credentials',   secretKey: 'nexus.username'),
      secretEnvVar(key: 'NEXUS_PASSWORD',               secretName: 'infra-credentials',   secretKey: 'nexus.password')
  ] +
  pipConfig.defineEnvVars(this) +
  k8sConfig.defineEnvVars(this) +
  EnVars

  try
  {
    printParameters()

    podTemplate_basic_singleSlave(jenkinsNamespace, envVars) {
      node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
        stage('Apply k8s configs') {
          container('jenkins-slave') {
            checkout([
                $class: 'GitSCM',
                branches: [[name: "tags/$GITSCM_TAG"]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: GLOBAL_GITSCM_CREDSID, url: GITSCM_REPO_URL]]])

            k8sClient.context = AH_CLUSTER
            configClient.setup()

            def defaultSecrets = [
                'infra-secretconfigs',
                'infra-urls',
                'infra-credentials',
                'regsecret']

            configClient.copySecrets(
                source: jenkinsNamespace,
                destination: automationNamespace,
                names: defaultSecrets + sourceSecret)

            def configmaps = configClient.createConfigmaps(
                configDir: CONFIGS_ROOT,
                env: AH_CLUSTER,
                prefix: configPrefix,
                namespace: automationNamespace,
                patch: serviceConfig.configmapPatch)

            configClient.patchSecret(
                namespace: automationNamespace,
                name: sourceSecret,
                patch: serviceConfig.secretPatch)
                
            k8sClient.renameResource('secret', sourceSecret,
                targetSecret, automationNamespace)

            configClient.copyConfigmaps(
                source: automationNamespace,
                destination: AH_NAMESPACE,
                replace: true,
                names: configmaps.name)

            configClient.copySecrets(
                source: automationNamespace,
                destination: AH_NAMESPACE,
                replace: true,
                names: defaultSecrets + targetSecret)
          }
        }
      }
    }

    stage('Run Predeployment Script') {
      podTemplate_migration(JOB_BASE_NAME, BUILD_ID, automationNamespace, AH_CONFIGURATION_PATH, configPrefix, envVars) {
        node("slvm_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
          checkout([
              $class: 'GitSCM',
              branches: [[name: "tags/$GITSCM_TAG"]],
              doGenerateSubmoduleConfigurations: false,
              extensions: [],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: GLOBAL_GITSCM_CREDSID, url: GITSCM_REPO_URL]]
          ])

          config.preMigrationRunHook?.call()

          if (!config["skipMigrationRun"] && SKIP_PRE_DEPLOY == "false") {
            container('dotnetcore-sdk') {
                sh '''
                    set +x && echo "$NUGET_CONFIG" > NuGet.Config && set -x
                    chmod +x $PREDEPLOYMENT_SCRIPT_PATH && $PREDEPLOYMENT_SCRIPT_PATH
                '''
            }
          }

          config.postMigrationRunHook?.call()
        }
      }
    }

    stage('Deploy Service') {
      podTemplate_basic(jenkinsNamespace, envVars) {
        node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
          checkout([
              $class: 'GitSCM',
              branches: [[name: "tags/$GITSCM_TAG"]],
              doGenerateSubmoduleConfigurations: false,
              extensions: [],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: GLOBAL_GITSCM_CREDSID, url: GITSCM_REPO_URL]]
          ])

          config.preDeployHook?.call()

          if (!config["skipDeploy"]) {
            container('kube'){
              k8sClient.context = AH_CLUSTER

              def content = readFile(
                  file: SERVICE_YAML_TEMPLATE_PATH,
                  encoding: 'utf-8')

              def values = [
                AH_SCM_TAG: GITSCM_TAG,
                AH_SERVICE_NAME: serviceNameLower,
                SERVICE_NAME_LOWER: serviceNameLower,              
                SERVICE_IMAGE_LOWER: SERVICE_IMAGE.toLowerCase(),
                NEXUS_DOCKER_REGISTRY_URL: GLOBAL_NEXUS_DOCKER_REGISTRY_URL,
                GITSCM_TAG_NO_DOTS: GITSCM_TAG.replaceAll("\\.", "-"),
              ]

              def rendered = templateEngine.render(content, values)
              writeFile(
                file: SERVICE_YAML_TEMPLATE_PATH,
                encoding: 'utf-8',
                text: rendered
              )

              k8sClient.applyResource(path: SERVICE_YAML_TEMPLATE_PATH,
                  namespace: AH_NAMESPACE)
            }
          }

          config.postDeployHook?.call()
        }
      }
    }
  }

  finally {
    podTemplate_basic_singleSlave(jenkinsNamespace, envVars) {
      node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
        config.preCleanUpHook?.call()

        if (!config["skipCleanUp"]) {
          container('jenkins-slave') {
            stage('CleanUp') {
              k8sClient = new K8sClient(this)
              k8sClient.context = AH_CLUSTER
              k8sClient.deleteResource('namespace', automationNamespace,
                  ignoreNotFound: true)
              k8sClient.deleteOutdatedResources('configmaps', serviceNameLower, 10)
              k8sClient.deleteOutdatedResources('secrets', serviceNameLower, 5)
            }
          }
        }

        config.postCleanUpHook?.call()
      }
    }
  }
}
