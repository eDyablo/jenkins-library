#!groovy
def call(
  namespace, 
  envVars, 
  body
)
{
  println "podTemplate_basic:"
  println "\nParameter: JOB_BASE_NAME => ${JOB_BASE_NAME}"
  println "\nParameter: BUILD_ID => ${BUILD_ID}"
  println "\nParameter: namespace => ${namespace}"
  
  podTemplate(
    cloud: "kubernetes-dev",
    label: "slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase(),
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
        image: 'artifacts.k8s.us-west-2.dev.e4d.com:8082/jenkins-slave:1.7.6',
        ttyEnabled: true,
        command: 'cat')
    ],
    imagePullSecrets: [ 'regsecret' ],
    volumes: [
      hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
    ]
  ) 
  {
    body()
  }
}
