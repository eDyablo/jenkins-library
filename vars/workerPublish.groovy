#!groovy

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

  boolean pullRequestBranch = BRANCH_NAME ==~ /origin\/pr\/.*/
  boolean publishRequested = params.ISPUBLISH == 'true' && !pullRequestBranch
  boolean canPublish = env.JOB_NAME ==~ /e4d\/PostMergeJobs\/.+/

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
    def AUTOMATION_NAMESPACE    = "automation-$SERVICE_NAME-$BUILD_ID".toLowerCase()
    def CONFIGPREFIX            = "${SERVICE_NAME}-${VERSION}".toLowerCase()
    def SERVICE_NAME_LOWER      = "${SERVICE_NAME}".toLowerCase()
    def IS_DEBUG                = Boolean.toString("${BUILD_CONFIGURATION}".toLowerCase() == 'debug')
    SERVICE_DEFAULT_SECRET_NAME = SERVICE_SECRET_NAME ?: "$SERVICE_NAME-secret".toLowerCase()
    def SERVICE_SECRET_NAME_KEY_2OVERRIDE = "secret.json"
    def LOCAL_OVERRIDEN_SECRETS = OVERRIDEN_SECRETS

    def envVars = [
      envVar(key: 'AH_SCM_TAG',                         value: GITSCM_TAG),
      envVar(key: 'AH_SERVICE_NAME',                    value: SERVICE_NAME),
      envVar(key: 'AH_CLUSTER',                         value: AH_CLUSTER),
      envVar(key: 'AH_NAMESPACE',                       value: AH_NAMESPACE),
      envVar(key: 'AH_CONFIGURATION_PATH',              value: AH_CONFIGURATION_PATH),
      envVar(key: 'AH_CONFIGURATION_DEBUG',             value: AH_CONFIGURATION_DEBUG),
      
      envVar(key: 'SERVICE_NAME_LOWER',                 value: SERVICE_NAME_LOWER),
      envVar(key: 'SERVICE_DEFAULT_SECRET_NAME',        value: SERVICE_DEFAULT_SECRET_NAME),
      envVar(key: 'SERVICE_SECRET_NAME_KEY_2OVERRIDE',  value: SERVICE_SECRET_NAME_KEY_2OVERRIDE),
      envVar(key: 'SERVICE_IMAGE',                      value: SERVICE_IMAGE),
      envVar(key: 'PROJECT_PATH',                       value: SERVICE_PROJECT_PATH),
      envVar(key: 'BUILD_CONFIGURATION',                value: BUILD_CONFIGURATION),
      envVar(key: 'GITSCM_REPO_URL',                    value: GITSCM_REPO_URL),
      envVar(key: 'BRANCH_NAME',                        value: BRANCH_NAME),
      envVar(key: 'E4D_BUILD_ID_BASE',               value: E4D_BUILD_ID_BASE),      
      envVar(key: 'GIT_USERNAME',                       value: GIT_USERNAME),
      envVar(key: 'GIT_PASSWORD',                       value: GIT_PASSWORD),
      envVar(key: 'VERSION',                            value: VERSION),
      envVar(key: 'CONFIGPREFIX',                       value: CONFIGPREFIX),
      envVar(key: 'BUILD_ID',                           value: BUILD_ID),
      envVar(key: 'ISPUBLISH',                          value: ISPUBLISH),
      envVar(key: 'JENKINS_NAMESPACE',                  value: JENKINS_NAMESPACE),
      envVar(key: 'AUTOMATION_NAMESPACE',               value: AUTOMATION_NAMESPACE),
      envVar(key: 'CONFIGS_ROOT',                       value: CONFIGS_ROOT),
      envVar(key: 'OVERRIDEN_CONFIGS',                  value: OVERRIDEN_CONFIGS),
      envVar(key: 'LOCAL_OVERRIDEN_SECRETS',            value: LOCAL_OVERRIDEN_SECRETS),
      envVar(key: 'JOB_BASE_NAME',                      value: JOB_BASE_NAME),

      envVar(key: 'NEXUS_REPO_URL',                     value: GLOBAL_NEXUS_REPO_URL),
      envVar(key: 'NEXUS_PIP_REPO_URL',                 value: GLOBAL_NEXUS_PIP_REPO_URL),
      envVar(key: 'GITSCM_CREDSID',                     value: GLOBAL_GITSCM_CREDSID),

      secretEnvVar(key: 'NUGET_CONFIG',                 secretName: 'infra-secretconfigs', secretKey: 'nuget.config'),
      secretEnvVar(key: 'KUBE_CONFIG',                  secretName: 'infra-secretconfigs', secretKey: 'kube.config'),
      secretEnvVar(key: 'NEXUS_APIKEY',                 secretName: 'infra-credentials',   secretKey: 'nexus.apikey'),
      secretEnvVar(key: 'NEXUS_USER',                   secretName: 'infra-credentials',   secretKey: 'nexus.username'),
      secretEnvVar(key: 'NEXUS_PASSWORD',               secretName: 'infra-credentials',   secretKey: 'nexus.password')
    ] + EnVars
 
    try {
      printParameters()

      podTemplate_basic(JENKINS_NAMESPACE, envVars) {
        node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
          stage('Pull') {
            checkoutSCM(BRANCH_NAME, GLOBAL_GITSCM_CREDSID, GITSCM_REPO_URL)
          }
          if (!config.skipUnitTests) {
            stage('Run Unit Tests') {
              if (config.preUnitTestHook) {
                config.preUnitTestHook()
              }
              dotnetTest(projectDir: SERVICE_PROJECT_PATH + '.Tests')
              if (config.postUnitTestHook) {
                config.postUnitTestHook()
              }
            }
          }
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
            if (config.preBuildHook) {
              config.preBuildHook()
            }
            if (!config.skipDockerBuildAndPublish) {
              stage('Build docker') {
                dotnetPublish(
                  projectDir: SERVICE_PROJECT_PATH,
                  buildConfig: BUILD_CONFIGURATION
                )
              }
            }
            if (config.postBuildHook) {
              config.postBuildHook()
            }
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
              if (config.preDockerPushHook) {
                config.preDockerPushHook()
              }
              if (config.postDockerPushHook) {
                config.postDockerPushHook()
              }
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
                      git tag -a $VERSION -m "Jenkins: Worker image version [ci skip] ."
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
    }
    catch (all) {
      error(all.message)
    }
    finally {
      if (AUTOMATION_NAMESPACE ==~ /automation.*/) {
        podTemplate_basic(JENKINS_NAMESPACE, envVars) {
          node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
            if (config.preCleanUpHook) {
              config.preCleanUpHook()
            }
            if (!config["skipCleanUp"]) {
              stage('CleanUp') {
                container('kube') {
                  sh '''
                    set +x && mkdir ~/.kube && echo "$KUBE_CONFIG" > ~/.kube/config && set -x
                    kubectl config get-contexts
                    kubectl config use-context default
                    
                    if [ ! -z "$(kubectl get namespace | grep $AUTOMATION_NAMESPACE)" ];
                    then                                                              
                      echo "delete $AUTOMATION_NAMESPACE namespace"
                      kubectl delete namespace $AUTOMATION_NAMESPACE
                    fi
                  '''
                }
              }
            }
            if (config.postCleanUpHook) {
              config.postCleanUpHook()
            }
          }
        }
      }
    }
  }
}
