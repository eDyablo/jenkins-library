package com.e4d.k8s

import hudson.model.*
import com.cloudbees.groovy.cps.NonCPS

class K8sConfig {
  K8sConfigReference configRef = new K8sConfigReference()

  final static String CONFIG_FILE_ENV_VAR = 'KUBECONFIG'

  @NonCPS
  void setConfigRef(String value) {
    this.configRef = K8sConfigReference.fromText(value)
  }

  def defineParameters(pipeline) {
    [
      new StringParameterDefinition('KUBE_CONFIG_REF', "${ configRef }",
        'Specify k8s configuration used as reference to secret contains the configuration and name of context in form of column separated string \'secret-name : secret-key : context-name\''),
    ]
  }

  void loadParameters(params) {
    configRef = params.KUBE_CONFIG_REF ?: "${ configRef }"
  }

  def defineEnvVars(pipeline) {
    [
      pipeline.envVar(key: CONFIG_FILE_ENV_VAR, value: configPath)
    ]
  }

  def defineVolumes(pipeline) {
    [
      pipeline.secretVolume(
        secretName: configRef.name,
        mountPath: [configDir, configRef.name].join('/'),
      ),
    ]
  }

  @NonCPS
  @Override
  String toString() {
    "[configRef: '${configRef}']"
  }

  @NonCPS getConfigDir() {
    ['/etc', this.class.name].join('/')
  }

  @NonCPS
  String getConfigPath() {
    [configDir, configRef.name, configRef.key].findAll().join('/')
  }
}
