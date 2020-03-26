package com.e4d.dotnet

import com.e4d.build.SecretReference
import com.cloudbees.groovy.cps.NonCPS

class DotnetConfig {
  SecretReference nugetConfigRef = new SecretReference()

  @NonCPS
  void setNugetConfigRef(SecretReference value) {
    nugetConfigRef = value
  }

  void setNugetConfigRef(String value) {
    nugetConfigRef = SecretReference.fromText(value)
  }

  def defineParameters(pipeline) {
    [ pipeline.string(name: 'DOTNET_NUGET_CONFIG_REF',
        defaultValue: "$nugetConfigRef",
        description: 'Reference to secret contains configuration for nuget package manager') ]
  }

  def loadParameters(params) {
    nugetConfigRef = params.DOTNET_NUGET_CONFIG_REF
  }

  def defineEnvVars(pipeline) {
    [ pipeline.secretEnvVar(key: 'NUGET_CONFIG',
        secretName: nugetConfigRef.name, secretKey: nugetConfigRef.key) ]
  }
}

