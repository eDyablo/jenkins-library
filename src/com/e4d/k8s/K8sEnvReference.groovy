package com.e4d.k8s

import com.cloudbees.groovy.cps.NonCPS

class K8sEnvReference {
  final String context
  final String namespace

  static final String delimiter = ':'
  static final String defaultContext = 'default'
  static final String defaultNamespace = 'default'

  K8sEnvReference() {
    context = defaultContext
    namespace = defaultNamespace
  }

  K8sEnvReference(String context) {
    this.context = context ?: defaultContext
    namespace = defaultNamespace
  }

  K8sEnvReference(String context, String namespace) {
    this.context = context ?: defaultContext
    this.namespace = namespace ?: defaultNamespace
  }

  @NonCPS
  static K8sEnvReference fromText(String text) {
    def tokens = text?.trim()?.split(delimiter, 2).findAll() ?: []
    new K8sEnvReference(
      tokens.take(1)[0]?.trim(),
      tokens.drop(1)[0]?.trim())
  }

  @NonCPS
  @Override
  String toString() {
    [context, namespace].join(delimiter)
  }

  String pretty() {
    [context, namespace].findAll{ it }.join(" ${ delimiter } ")
  }
}
