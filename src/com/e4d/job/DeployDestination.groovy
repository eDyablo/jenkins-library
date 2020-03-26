package com.e4d.job

import com.cloudbees.groovy.cps.NonCPS

class DeployDestination {
  String context
  String namespace

  static final String delimiter = ':'
  static final String defaultContext = 'default'
  static final String defaultNamespace = 'default'

  @NonCPS
  static DeployDestination fromText(String text) {
    def tokens = text?.trim()?.split(delimiter, 2).findAll() ?: []
    new DeployDestination(
      context: tokens.take(1)[0]?.trim() ?: defaultContext,
      namespace: tokens.drop(1)[0]?.trim() ?: defaultNamespace)
  }

  @NonCPS
  @Override
  String toString() {
    [context, namespace].join(delimiter)
  }

  String pretty() {
    [context, namespace].findAll{ it }.join(" $delimiter ")
  }
}
