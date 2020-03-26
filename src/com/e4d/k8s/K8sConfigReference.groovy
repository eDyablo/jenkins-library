package com.e4d.k8s

import com.e4d.build.SecretReference
import com.cloudbees.groovy.cps.NonCPS

class K8sConfigReference extends SecretReference {
  String context

  @NonCPS
  static K8sConfigReference fromText(String text) {
    def tokens = text?.tokenize(':') ?: []
    tokens = tokens.collect { it.trim() }
    def reference = new K8sConfigReference(name: tokens[0], key: tokens[0],
        context: 'default')
    def tokensNumber = tokens.size()
    if (tokensNumber > 1)
      reference.key = tokens[1]
    if (tokensNumber > 2)
      reference.context = tokens[2]
    return reference
  }

  @NonCPS
  @Override
  String toString() {
    return [super.toString(), context].join(' : ')
  }
}
