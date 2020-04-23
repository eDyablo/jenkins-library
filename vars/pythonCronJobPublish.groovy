#!groovy

def call(
  BRANCH_NAME,
  EnVars = [],
  body
)
{
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  withCredentials([
      usernamePassword(credentialsId: "${GLOBAL_GITSCM_CREDSID}",
      usernameVariable: 'GIT_USERNAME',
      passwordVariable: 'GIT_PASSWORD')
    ]) {
    def VERSION = "$E4D_BUILD_ID_BASE.$BUILD_ID"
    if (BRANCH_NAME ==~ /origin\/pr\/.*/) {
      VERSION = "$E4D_BUILD_ID_BASE.$BUILD_ID-pr"
    }

    def JENKINS_NAMESPACE       = "jenkins"
    def AUTOMATION_NAMESPACE    = "automation-$SERVICE_NAME-$BUILD_ID".toLowerCase()
    def CONFIGPREFIX            = "${SERVICE_NAME}-${VERSION}".toLowerCase()
    def SERVICE_NAME_LOWER      = "${SERVICE_NAME}".toLowerCase()
    SERVICE_DEFAULT_SECRET_NAME = SERVICE_SECRET_NAME ?: "$SERVICE_NAME-secret".toLowerCase()
    def SERVICE_SECRET_NAME_KEY_2OVERRIDE = "secret.json"

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
      envVar(key: 'PROJECT_PATH',                       value: PROJECT_PATH),
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
      envVar(key: 'JOB_BASE_NAME',                      value: JOB_BASE_NAME),
      envVar(key: 'LOCAL_OVERRIDEN_CONFIGS',            value: LOCAL_OVERRIDEN_CONFIGS),
      envVar(key: 'LOCAL_OVERRIDEN_SECRETS',            value: LOCAL_OVERRIDEN_SECRETS),
      envVar(key: 'NEXUS_REPO_URL',                     value: GLOBAL_NEXUS_REPO_URL),
      envVar(key: 'NEXUS_PIP_REPO_URL',                 value: GLOBAL_NEXUS_PIP_REPO_URL),
      envVar(key: 'GITSCM_CREDSID',                     value: GLOBAL_GITSCM_CREDSID),
      secretEnvVar(key: 'PIP_CONFIG',                   secretName: 'infra-secretconfigs', secretKey: 'pip.config'),
      secretEnvVar(key: 'NUGET_CONFIG',                 secretName: 'infra-secretconfigs', secretKey: 'nuget.config'),
      secretEnvVar(key: 'KUBE_CONFIG',                  secretName: 'infra-secretconfigs', secretKey: 'kube.config'),
      secretEnvVar(key: 'NEXUS_APIKEY',                 secretName: 'infra-credentials',   secretKey: 'nexus.apikey'),
      secretEnvVar(key: 'NEXUS_USER',                   secretName: 'infra-credentials',   secretKey: 'nexus.username'),
      secretEnvVar(key: 'NEXUS_PASSWORD',               secretName: 'infra-credentials',   secretKey: 'nexus.password')
    ] + EnVars

    try {
      podTemplate_basic_singleSlave("${JENKINS_NAMESPACE}", envVars) {
        node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
          stage('Pull') {
            checkoutSCM("${BRANCH_NAME}", "${GLOBAL_GITSCM_CREDSID}", "${GITSCM_REPO_URL}")
          }

          if (config.preUnitTestHook)
          {
            config.preUnitTestHook.delegate = this
            config.preUnitTestHook.resolveStrategy = Closure.DELEGATE_FIRST
            config.preUnitTestHook()
          }

          container('jenkins-slave') {
            stage('Run Unit Tests') {
              sh'''
                echo "Configure Python"
                SOURCEROOT=$(pwd) && cd $PROJECT_PATH
                export PYTHONPATH=/usr/local/bin/
                export PYTHONPATH="$PYTHONPATH:$(pwd)"

                if [ -f "requirements.txt" ]; then
                  set +x && echo "$PIP_CONFIG" > /etc/pip.conf && set -x
                  pip install -r requirements.txt
                fi

                python -m unittest discover -s ./unit_tests -p "*_test.py"
              '''
            }
          }

          if (config.postUnitTestHook)
          {
            config.postUnitTestHook.delegate = this
            config.postUnitTestHook.resolveStrategy = Closure.DELEGATE_FIRST
            config.postUnitTestHook()
          }

          if (SKIP_INTEGRATION_TESTS == "false") {
            def hookArgs = [
              serviceName: SERVICE_NAME_LOWER,
              namespace: AUTOMATION_NAMESPACE,
              version: VERSION
            ]
            config.preApplyK8SConfigsHook?.call(hookArgs)
            container('jenkins-slave') {
              stage('Apply K8S Configs') {
                sh'''
                  echo "Configure Python"
                  set +x && export PYTHONPATH=/usr/local/bin/ && set -x
                  set +x && echo "$PIP_CONFIG" > /etc/pip.conf && set -x

                  echo "Configuring kubectl"
                  curl -LO https://storage.googleapis.com/kubernetes-release/release/v1.15.3/bin/linux/amd64/kubectl
                  chmod +x ./kubectl && mv ./kubectl /usr/local/bin/kubectl && mkdir ~/.kube
                  set +x && echo "$KUBE_CONFIG" > ~/.kube/config && set -x

                  echo "Creating $AUTOMATION_NAMESPACE and copying secrets from jenkins"
                  if [ -z "$(kubectl get namespace | grep $AUTOMATION_NAMESPACE)" ];
                  then
                    kubectl create namespace $AUTOMATION_NAMESPACE
                  fi
                  kubectl get secrets -o json --namespace $JENKINS_NAMESPACE | jq --arg NAMESPACE "$AUTOMATION_NAMESPACE" '.items[].metadata.namespace = $NAMESPACE' | kubectl create -f  -

                  echo "Prepare configandsecrets tool"
                  pip3 install configandsecrets

                  echo "Copying service secret to $AUTOMATION_NAMESPACE and patch if OVERRIDEN_SECRET !=null"
                  service_secret=$(kubectl get secrets $SERVICE_DEFAULT_SECRET_NAME -o json --namespace $JENKINS_NAMESPACE)
                  echo "$service_secret" | jq --arg target_namespace "$AUTOMATION_NAMESPACE" --arg target_name "$SERVICE_NAME_LOWER-$VERSION-secret" '.metadata.namespace = $target_namespace | .metadata.name = $target_name' | kubectl create -f  -
                '''

                sh """
                  set +x && export PYTHONPATH=/usr/local/bin/ && set -x
                  python -m create_config ${AH_CONFIGURATION_PATH} --map-name=${SERVICE_NAME_LOWER}-${VERSION}-config --env-map-name=${SERVICE_NAME_LOWER}-${VERSION}-env --env=test ${LOCAL_OVERRIDEN_CONFIGS} | kubectl create --namespace=${AUTOMATION_NAMESPACE} --filename=-

                  if [ ! -z ${LOCAL_OVERRIDEN_SECRETS} ]; then
                      python -m patch_config secret ${SERVICE_NAME_LOWER}-${VERSION}-secret  ${SERVICE_SECRET_NAME_KEY_2OVERRIDE} ${LOCAL_OVERRIDEN_SECRETS} --namespace ${AUTOMATION_NAMESPACE}
                  fi
                """
              }
            }
            config.postApplyK8SConfigsHook?.call(hookArgs)
          }
        }
      }

      if (SKIP_INTEGRATION_TESTS == "false") {
        podTemplate_migration_singleSlave("${JOB_BASE_NAME}", "${BUILD_ID}", "${AUTOMATION_NAMESPACE}", "${AH_CONFIGURATION_PATH}", "${CONFIGPREFIX}", envVars) {
          node("slvm_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
            checkoutSCM("${BRANCH_NAME}", "${GLOBAL_GITSCM_CREDSID}", "${GITSCM_REPO_URL}")

            config.preIntegrationTestsRunHook?.call()

            container('jenkins-slave') {
              stage('Run Integration Tests') {
                sh'''
                  echo "Configure Python"
                  SOURCEROOT=$(pwd) && cd $PROJECT_PATH
                  export PYTHONPATH=/usr/local/bin/
                  export PYTHONPATH="$PYTHONPATH:$(pwd)"

                  if [ -f "requirements.txt" ]; then
                    set +x && echo "$PIP_CONFIG" > /etc/pip.conf && set -x
                    pip install -r requirements.txt
                  fi

                  python -m unittest discover -s ./integration_tests -p "*_test.py"
                '''
              }
            }

            config.postIntegrationTestsRunHook?.call()
          }
        }
      }

      if ((ISPUBLISH == 'true')  || !(BRANCH_NAME ==~ /origin\/pr\/.*/)) {
        podTemplate_basic_singleSlave("${JENKINS_NAMESPACE}", envVars + [envVar(key: 'VERSION', value: "${VERSION}")]) {
          node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
            checkoutSCM("${BRANCH_NAME}", "${GLOBAL_GITSCM_CREDSID}", "${GITSCM_REPO_URL}")

            config.preDockerPushHook?.call()

            container('jenkins-slave') {
              stage('Push to docker registry') {
                def buildArgs = DOCKERFILE_ARGS ?: ''
                sh '''
                  set +x && docker login -u $NEXUS_USER -p $NEXUS_PASSWORD $NEXUS_REPO_URL && set -x
                  NEXUS_REPO_SHORT_URL=${NEXUS_REPO_URL#"http://"}
                  IMAGE_NAME=$(echo $NEXUS_REPO_SHORT_URL/$SERVICE_IMAGE | awk '{print tolower($0)}')
                  cd $PROJECT_PATH/ && ls -la
                  set +x && echo "$PIP_CONFIG" > pip.conf && set -x
                ''' +
                "\ndocker build --network=host -f Dockerfile ${buildArgs} -t \$IMAGE_NAME:\$VERSION .\n" +
                '''
                    docker push $IMAGE_NAME:$VERSION
                    docker tag $IMAGE_NAME:$VERSION $IMAGE_NAME:latest
                    docker push $IMAGE_NAME:latest
                    docker images -a | grep $IMAGE_NAME | grep $VERSION | awk '{print $3}' | xargs docker rmi -f
                '''
              }
            }

            config.postDockerPushHook?.call()

            container('jenkins-slave') {
              stage('Tag SCM') {
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
        }
      }
    }
    finally {
      if (AUTOMATION_NAMESPACE ==~ /automation.*/) {
        podTemplate_basic_singleSlave("${JENKINS_NAMESPACE}", envVars) {
          node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {

            config.preCleanUpHook?.call()

            container('jenkins-slave') {
              stage('CleanUp') {
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

            config.postCleanUpHook?.call()
          }
        }
      }
    }
  }
}
