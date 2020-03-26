package com.e4d.service

import com.e4d.k8s.K8sEnvReference
import com.cloudbees.groovy.cps.NonCPS

class ServiceInstanceReference {
  final String name
  final K8sEnvReference env

  static final String delimiter = ':'

  ServiceInstanceReference(String name) {
    this.name = name
    env = new K8sEnvReference()
  }

  ServiceInstanceReference(String name, K8sEnvReference env) {
    this.name = name
    this.env = env ?: new K8sEnvReference()
  }

  @NonCPS
  static ServiceInstanceReference fromText(String text) {
    def tokens = text?.trim()?.split(delimiter, 2).findAll() ?: []
    new ServiceInstanceReference(
      tokens.take(1)[0]?.trim(),
      K8sEnvReference.fromText(tokens.drop(1)[0]?.trim()))
  }

  @NonCPS
  @Override
  String toString() {
    [name, env.toString()].join(delimiter)
  }

  String pretty() {
    [name, env.pretty()].findAll{ it }.join(" ${ delimiter } ")
  }
}
