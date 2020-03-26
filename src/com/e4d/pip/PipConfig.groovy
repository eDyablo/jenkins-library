package com.e4d.pip

import com.e4d.build.SecretReference
import com.cloudbees.groovy.cps.NonCPS

class PipConfig {
  SecretReference configRef = new SecretReference()

  final static String CONFIG_FILE_ENV_VAR = 'PIP_CONFIG_FILE'

  @NonCPS
  void setConfigRef(String value) {
    this.configRef = SecretReference.fromText(value)
  }

  @NonCPS
  String getConfigDir() {
    ['/etc', this.class.name].join('/')
  }

  @NonCPS
  String getConfigPath() {
    [configDir, configRef.name, configRef.key ].findAll().join('/')
  }

  def defineParameters(pipeline) {
    [ pipeline.string(name: 'PIP_CONFIG_REF', defaultValue: configRef.pretty(),
      description: 'Reference to secret contains the configuration for PIP in form of column separated string \'secret-name:secret-key\''), ]
  }

  void loadParameters(params) {
    configRef = params.PIP_CONFIG_REF ?: configRef.toString()
  }

  def defineEnvVars(pipeline) {
    [ pipeline.envVar(key: CONFIG_FILE_ENV_VAR, value: configPath) ]
  }

  def defineVolumes(pipeline) {
    [
      pipeline.secretVolume(
        secretName: configRef.name,
        mountPath: [configDir, configRef.name].join('/'),
      ),
    ]
  }
}
