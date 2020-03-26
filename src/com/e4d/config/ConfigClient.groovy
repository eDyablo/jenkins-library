package com.e4d.config

import com.e4d.shell.ShellClient
import com.e4d.k8s.K8sConfig
import com.e4d.pip.PipConfig

class ConfigClient extends ShellClient {
  K8sConfig k8sConfig
  PipConfig pipConfig

  ConfigClient(pipeline, K8sConfig k8sConfig, PipConfig pipConfig) {
    super(pipeline)
    this.k8sConfig = k8sConfig
    this.pipConfig = pipConfig
  }

  void setup() {
    execute(
      hidden(
        'export PYTHONPATH=/usr/local/bin',
        'echo "$PIP_CONFIG" > /etc/pip.conf'
      ),
      'pip install --upgrade pip',
      'pip install configandsecrets',
      'apt-get update && apt-get install jq')
  }

  @Deprecated
  void copySecrets(String source, String destination, List<String> names) {
    copySecrets(source: source, destination: destination, names: names)
  }

  void copySecrets(Map kwargs) {
    copyResources(kwargs + [kind: 'secret'])
  }

  void copyConfigmaps(Map kwargs) {
    copyResources(kwargs + [kind: 'configmap'])
  }

  void copyResources(Map kwargs) {
    def names = kwargs.names ?: []
    def source = kwargs.source
    def destination = kwargs.destination
    def namespaceSelector = names.size() > 1 ? '.items[].metadata.namespace' : '.metadata.namespace'
    def replace = kwargs.replace ?: false
    execute("if [ -z \"\$(kubectl get namespace | grep -w $destination)\" ]; then kubectl create namespace $destination; fi",
      piped("kubectl get $kwargs.kind ${names.join(' ')} --output json --namespace=$source",
        "jq --arg namespace $destination '$namespaceSelector=\$namespace'",
        "kubectl ${replace ? 'replace --force' : 'create'} --filename -"))
  }

  def createConfigmaps(Map kwargs) {
    def mapName = "$kwargs.prefix-config"
    def envMapName = "$kwargs.prefix-env"
    def overrides = kwargs.patch?.collect{ key, patch ->
      "--override $key='$patch'"
    }.join(' ')
    execute(
      hidden('export PYTHONPATH=/usr/local/bin'),
      piped(
        "python -m create_config $kwargs.configDir --map-name=$mapName --env-map-name=$envMapName --env=$kwargs.env $overrides",
        "kubectl create --namespace=$kwargs.namespace --filename=-"
      ))
    return [
      [name: mapName, namespace: kwargs.namespace],
      [name: envMapName, namespace: kwargs.namespace]
    ]
  }

  void patchConfigmap(Map kwargs) {
    patchConfig(kwargs + [kind: 'configmap'])
  }

  void patchSecret(Map kwargs) {
    patchConfig(kwargs + [kind: 'secret'])
  }

  void patchConfig(Map kwargs) {
    if (kwargs.patch) {
      execute(
        hidden('export PYTHONPATH=/usr/local/bin'),
        kwargs.patch.collect{ key, patch ->
          "python -m patch_config $kwargs.kind $kwargs.name $key '$patch' --namespace=$kwargs.namespace"
        }.join('\n')
      )
    }
  }
}
