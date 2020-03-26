#!groovy
def call(
  JOB_BASE_NAME,
  BUILD_ID,
  namespace, 
  AH_CONFIGURATION_PATH,
  CONFIGPREFIX,
  envVars,
  body
)
{
  println "podTemplate_migration:"
  println "\nParameter: JOB_BASE_NAME => ${JOB_BASE_NAME}"
  println "\nParameter: BUILD_ID => ${BUILD_ID}"
  println "\nParameter: namespace => ${namespace}"
  println "\nParameter: AH_CONFIGURATION_PATH => ${AH_CONFIGURATION_PATH}"
  println "\nParameter: CONFIGPREFIX => ${CONFIGPREFIX}"

 def cloud = (AH_CLUSTER == "default") ? "" : "-${AH_CLUSTER}"

  podTemplate(
    cloud: "kubernetes${cloud}",
    label: "slvm_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase(),
    annotations:  [
      podAnnotation(key: "iam.amazonaws.com/role", value: "RoleCiSlave")
    ],  
    envVars: envVars,
    namespace: "${namespace}",
    nodeUsageMode: 'EXCLUSIVE',
    nodeSelector: 'nodetype=slave',
    containers: [
      containerTemplate(
        name: 'jenkins-slave',
        image: 'artifacts.k8s.us-west-2.dev.e4d.com:8082/jenkins-slave:1.7.5',
        ttyEnabled: true,
        command: 'cat')
    ],
    imagePullSecrets: [ 'regsecret' ],
    volumes: [
      secretVolume(mountPath: "${AH_CONFIGURATION_PATH}/service-secret", secretName: "${CONFIGPREFIX}-secret"),
      configMapVolume(mountPath: "${AH_CONFIGURATION_PATH}/service-config", configMapName: "${CONFIGPREFIX}-config")
    ]
  )
  
  {
    body()
  }
}
