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
        name: 'jessie',
        image: 'debian:jessie-slim',
        ttyEnabled: true,
        command: 'cat'),
      containerTemplate(
        name: 'docker',
        image: 'docker:17.12.0',
        ttyEnabled: true,
        command: 'cat'),  
      containerTemplate(
        name: 'python',
        image: 'python:3.6-jessie',
        ttyEnabled: true,
        command: 'cat'),        
      containerTemplate(
        name: 'dotnetcore-sdk',
        image: 'artifacts.k8s.us-west-2.dev.e4d.com:8082/dotnetcore-sdk:3.1-1',
        ttyEnabled: true,
        command: 'cat'),        
      containerTemplate(
        name: 'kube',
        image: 'roffe/kubectl:v1.9.1',
        ttyEnabled: true,
        command: 'cat'),
      containerTemplate(
        name: 'node',
        image: 'node:9.6.1',
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
