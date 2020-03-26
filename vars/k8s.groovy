#!groovy

import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput

def defEnvVar(Map args) {
  return [
    key: args.key,
    var: envVar(key: args.key, value: args.value)
  ]
}

def defSecretEnvVar(Map args) {
  return [
    key: args.key,
    var: secretEnvVar(key: args.key, secretName: args.secretName, secretKey: args.secretKey)
  ]
}

def configure(Map args) {
  return shell.execute("""
    curl -LO https://storage.googleapis.com/kubernetes-release/release/v1.8.6/bin/linux/amd64/kubectl
    chmod +x ./kubectl && mv ./kubectl /usr/local/bin/kubectl && mkdir ~/.kube
    set +x && echo "\$${args.config.key}" > ~/.kube/config && set -x
    kubectl config use-context ${args.context}
  """)
}

class Resource {
  Map data
  
  Resource(Map data) {
    this.data = data
  }
  
  def String toString() {
    return JsonOutput.toJson(data)
  }
  
  def String pretty() {
    return JsonOutput.prettyPrint(toString())
  }
}

def getConfigMap(String name, String namespace = 'default') {
  return getResource('configmaps', name, namespace)
}

def getSecret(String name, String namespace = 'default') {
  return getResource('secrets', name, namespace)
}

def getSecrets(ArrayList<String> names, String namespace = 'default') {
  return getResources('secrets', names, namespace)
}

def getResources(String kind, ArrayList<String> names, String namespace = 'default') {
  def resources = []
  names.each { name ->
    resources.add(getResource(kind, name, namespace))
  }
  return resources
}

def getResource(String kind, String name, String namespace = 'default') {
  def rawResource = getRawResource(kind, name, namespace)
  return new Resource(new JsonSlurperClassic().parseText(rawResource))
}

def getRawResource(String kind, String name, String namespace = 'default') {
  return shell.eval("kubectl get ${kind} ${name} --namespace=${namespace} --output=json")
}

def applyResource(Resource resource) {
  return shell.eval("""
    kubectl delete ${resource.data.kind} ${resource.data.metadata.name}  --namespace=${resource.data.metadata.namespace} --ignore-not-found
    echo \"${resource}\" | kubectl create --filename=-
  """)
}
