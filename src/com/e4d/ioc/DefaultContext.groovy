package com.e4d.ioc

import com.cloudbees.groovy.cps.NonCPS

class DefaultContext implements Context, Serializable {
  private def _pipeline

  DefaultContext(pipeline) {
    _pipeline = pipeline
  }

  @NonCPS
  @Override
  def getPipeline() {
    _pipeline
  }
}
