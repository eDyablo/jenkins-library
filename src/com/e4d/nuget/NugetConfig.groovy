package com.e4d.nuget

import com.e4d.build.SecretReference
import com.cloudbees.groovy.cps.NonCPS

class NugetConfig {
  SecretReference configRef = new SecretReference()

  static final String CONFIG_FILE_ENV_VAR = 'NUGET_CONFIG_FILE'
  
  @NonCPS
  String getConfigDir() {
    ['/etc', this.class.name].join('/')
  }

  @NonCPS
  String getConfigPath() {
    [configDir, configRef.name, configRef.key].findAll().join('/')
  }

  @NonCPS
  void setConfigRef(String value) {
    this.configRef = SecretReference.fromText(value)
  }

  def defineParameters(pipeline) {
    [ pipeline.string(name: 'nuget', defaultValue: configRef.pretty(),
      description: 'Reference to secret contains the configuration for Nuget in form of column separated string \'secret-name:secret-key\''), ]
  }

  void loadParameters(params) {
    configRef = params.nuget ?: configRef.toString()
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
