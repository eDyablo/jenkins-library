#!groovy

def call(
    EnVars = [],
    body
)
{
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def JENKINS_NAMESPACE       = "jenkins"
  def AUTOMATION_NAMESPACE    = "automation-$SERVICE_NAME-$BUILD_ID".toLowerCase()
  def CONFIGPREFIX            = "${SERVICE_NAME}-${GITSCM_TAG}".toLowerCase()
  def SERVICE_NAME_LOWER      = "${SERVICE_NAME}".toLowerCase()
  SERVICE_DEFAULT_SECRET_NAME = SERVICE_SECRET_NAME ?: "$SERVICE_NAME-secret".toLowerCase()

  def envVars = [
    envVar(key: 'AH_SCM_TAG',                         value: GITSCM_TAG),
    envVar(key: 'AH_SERVICE_NAME',                    value: SERVICE_NAME_LOWER),
    envVar(key: 'AH_CLUSTER',                         value: AH_CLUSTER),
    envVar(key: 'AH_NAMESPACE',                       value: AH_NAMESPACE),
    envVar(key: 'AH_CONFIGURATION_PATH',              value: AH_CONFIGURATION_PATH),
    envVar(key: 'AH_CONFIGURATION_DEBUG',             value: AH_CONFIGURATION_DEBUG),
    envVar(key: 'SERVICE_ARN_AWS_IAM',                value: config.SERVICE_ARN_AWS_IAM),
    envVar(key: 'SERVICE_IMAGE',                      value: SERVICE_IMAGE),
    envVar(key: 'SERVICE_NAME_LOWER',                 value: SERVICE_NAME_LOWER),
    envVar(key: 'SERVICE_YAML_TEMPLATE_PATH',         value: SERVICE_YAML_TEMPLATE_PATH),
    envVar(key: 'SERVICE_DEFAULT_SECRET_NAME',        value: SERVICE_DEFAULT_SECRET_NAME),
    envVar(key: 'GITSCM_TAG',                         value: GITSCM_TAG),
    envVar(key: 'GITSCM_TAG_NO_DOTS',                 value: GITSCM_TAG.replaceAll("\\.", "-")),
    envVar(key: 'JOB_BASE_NAME',                      value: JOB_BASE_NAME),
    envVar(key: 'CONFIGPREFIX',                       value: CONFIGPREFIX),
    envVar(key: 'BUILD_ID',                           value: BUILD_ID),
    envVar(key: 'JENKINS_NAMESPACE',                  value: JENKINS_NAMESPACE),      
    envVar(key: 'GITSCM_REPO_URL',                    value: GITSCM_REPO_URL),
    envVar(key: 'AUTOMATION_NAMESPACE',               value: AUTOMATION_NAMESPACE),
    envVar(key: 'NEXUS_DOCKER_REGISTRY_URL',          value: GLOBAL_NEXUS_DOCKER_REGISTRY_URL),
    envVar(key: 'NEXUS_PIP_REPO_URL',                 value: GLOBAL_NEXUS_PIP_REPO_URL),
    envVar(key: 'GITSCM_CREDSID',                     value: GLOBAL_GITSCM_CREDSID),
    secretEnvVar(key: 'NUGET_CONFIG',                 secretName: 'infra-secretconfigs', secretKey: 'nuget.config'),
    secretEnvVar(key: 'KUBE_CONFIG',                  secretName: 'infra-secretconfigs', secretKey: 'kube.config'),
    secretEnvVar(key: 'PYPI_CONFIG',                  secretName: 'infra-secretconfigs', secretKey: 'pypi.config'),
    secretEnvVar(key: 'NEXUS_APIKEY',                 secretName: 'infra-credentials',   secretKey: 'nexus.apikey'),
    secretEnvVar(key: 'NEXUS_USER',                   secretName: 'infra-credentials',   secretKey: 'nexus.username'),
    secretEnvVar(key: 'NEXUS_PASSWORD',               secretName: 'infra-credentials',   secretKey: 'nexus.password')
  ] + EnVars

  try
  {
    printParameters()

    stage('Pull and apply k8s configs') {
      podTemplate_basic_singleSlave("${JENKINS_NAMESPACE}", envVars) {
        node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
          stage('Pull') {
            checkout([
              $class: 'GitSCM',
              branches: [[name: "tags/${GITSCM_TAG}"]],
              doGenerateSubmoduleConfigurations: false,
              extensions: [],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "${GLOBAL_GITSCM_CREDSID}", url: "${GITSCM_REPO_URL}"]]
            ])
          }

          def hookArgs = [
            serviceName: SERVICE_NAME_LOWER,
            namespace: AH_NAMESPACE,
            version: GITSCM_TAG
          ]

          config.preConfiguringKubectlHook?.call(hookArgs)

          if (!config.skipConfiguring) {
            container('jenkins-slave') {
              sh '''
                echo "Configuring kubectl"
                curl -LO https://storage.googleapis.com/kubernetes-release/release/v1.8.6/bin/linux/amd64/kubectl
                chmod +x ./kubectl && mv ./kubectl /usr/local/bin/kubectl && mkdir ~/.kube
                set +x && echo "$KUBE_CONFIG" > ~/.kube/config && set -x

                kubectl config use-context $AH_CLUSTER 

                echo "Create destintaion namespace if absent and temporary migration namespace"
                kubectl create namespace $AUTOMATION_NAMESPACE

                kubectl config use-context $AH_CLUSTER
                if [ -z "$(kubectl get namespace | grep $AH_NAMESPACE)" ]; then
                  kubectl create namespace $AH_NAMESPACE
                fi

                apt-get update && apt-get install jq

                echo "preparation for $AH_NAMESPACE $AUTOMATION_NAMESPACE"
                
                set +x
                jenkins_secrets=$(kubectl get secrets infra-secretconfigs infra-urls infra-credentials regsecret -o json --namespace $JENKINS_NAMESPACE | jq --arg NAMESPACE "$AUTOMATION_NAMESPACE" '.items[].metadata.namespace = $NAMESPACE')
                echo "default microservice secret: $SERVICE_DEFAULT_SECRET_NAME"
                service_secret=$(kubectl get secrets $SERVICE_DEFAULT_SECRET_NAME -o json --namespace $JENKINS_NAMESPACE)
                set -x

                for namespace in $AH_NAMESPACE $AUTOMATION_NAMESPACE;
                do
                  echo "removing existing configMaps and secret for $namespace"
                  kubectl delete configmap --namespace=$namespace $SERVICE_NAME_LOWER-$GITSCM_TAG-config --ignore-not-found
                  kubectl delete configmap --namespace=$namespace $SERVICE_NAME_LOWER-$GITSCM_TAG-env --ignore-not-found
                  kubectl delete secret --namespace=$namespace $SERVICE_NAME_LOWER-$GITSCM_TAG-secret --ignore-not-found
                  
                  echo "Copying service secret to $namespace and patch if OVERRIDEN_SECRET !=null"
                  echo "$service_secret" | jq --arg target_namespace "$namespace" --arg target_name "$SERVICE_NAME_LOWER-$GITSCM_TAG-secret" '.metadata.namespace = $target_namespace | .metadata.name = $target_name' | kubectl create -f  - 
                done
                
                if [ -z "$(kubectl get secrets --namespace=$AH_NAMESPACE | grep regsecret)" ]; then
                  echo "copy regsecret to $AH_NAMESPACE if absent"
                  kubectl get secrets regsecret -o json --namespace $JENKINS_NAMESPACE | jq --arg NAMESPACE "$AH_NAMESPACE" '.metadata.namespace = $NAMESPACE' | kubectl create -f  -
                fi

                set +x
                echo "$jenkins_secrets" | kubectl create -f  -  
                set -x
              '''
            }
          }

          config.postConfiguringKubectlHook?.call(hookArgs)
        }
      }
    }

    stage('Deploy Service') {
      podTemplate_basic_singleSlave("${JENKINS_NAMESPACE}", envVars) {
        node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
          checkout([
            $class: 'GitSCM',
            branches: [[name: "tags/${GITSCM_TAG}"]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [],
            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: "${GLOBAL_GITSCM_CREDSID}", url: "${GITSCM_REPO_URL}"]]
          ])

          config.preDeployHook?.call()

          if (!config.skipDeploy) {
            container('jenkins-slave') {
              sh '''
                echo "checking variables in YAML template:"
                SERVICE_NAME_LOWER=$(echo "$SERVICE_NAME" | awk '{print tolower($0)}')
                SERVICE_IMAGE_LOWER=$(echo "$SERVICE_IMAGE" | awk '{print tolower($0)}')
                for placeholder in $(grep -wo '${[A-Z_]*}' ${SERVICE_YAML_TEMPLATE_PATH} | sort | uniq);
                do
                  variable=$(echo $placeholder | sed 's/{//' | sed 's/}//')
                  variable_value=$NULL
                  eval "variable_value=$variable"
                  if [ -z "$variable_value" ]; then
                    echo "FAILED '$'variable_value is empty or not passed via params" 1>&2
                    exit 1
                  else
                    sed -i "s#$placeholder#$variable_value#g" ${SERVICE_YAML_TEMPLATE_PATH}
                  fi
                done
                echo "All placeholders are replaced"

                echo "configuring kube config to work with needed context"
                mkdir ~/.kube && set +x && echo "$KUBE_CONFIG" > ~/.kube/config && set -x

                kubectl config use-context $AH_CLUSTER

                echo "applying deployment to $AH_CLUSTER context "
                kubectl apply -f ${SERVICE_YAML_TEMPLATE_PATH} --namespace=$AH_NAMESPACE
              '''
            }
          }

          config.postDeployHook?.call()
        }
      }
    }
  }
  finally {
    if (AUTOMATION_NAMESPACE ==~ /automation.*/) {
      podTemplate_basic_singleSlave("${JENKINS_NAMESPACE}", envVars) {
        node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {

          config.preCleanUpHook?.call()

          if (!config.skipCleanUp) {
            container('jenkins-slave') {
              stage('CleanUp') {
                sh '''                         
                  set +x && mkdir ~/.kube && echo "$KUBE_CONFIG" > ~/.kube/config && set -x
                  echo "Switching to $AH_CLUSTER context"
                  kubectl config use-context $AH_CLUSTER 
                  
                  if [ ! -z "$(kubectl get namespace | grep $AUTOMATION_NAMESPACE)" ];
                  then                                                              
                    echo "delete $AUTOMATION_NAMESPACE namespace"
                    kubectl delete namespace $AUTOMATION_NAMESPACE
                  fi
                '''
              }
            }
          }

          config.postCleanUpHook?.call()
        }
      }
    }
  }
}
