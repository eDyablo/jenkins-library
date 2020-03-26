package com.e4d.service

import com.cloudbees.groovy.cps.NonCPS

class PythonServiceConfig extends ServiceConfig {
  @NonCPS
  @Override
  def defineParameters(pipeline) {
    super.defineParameters(pipeline)
  }

  @NonCPS
  @Override
  def loadParameters(params) {
    super.loadParameters(params)
  }
}
