package com.e4d.service

import com.cloudbees.groovy.cps.NonCPS

class ServiceReference {
  String name
  String sourceName
  String imageName
  String deploymentName

  @NonCPS
  static ServiceReference fromText(String text) {
    def tokens = text?.tokenize(':') ?: []
    tokens = tokens.collect { it.trim() }
    def reference = new ServiceReference(name: tokens[0],
      sourceName: tokens[0], imageName: tokens[0],
      deploymentName: tokens[0])
    def tokensNumber = tokens.size()
    if (tokensNumber > 0)
      reference.name = tokens[0]
    if (tokensNumber > 1)
      reference.sourceName = tokens[1]
    if (tokensNumber > 2) {
      reference.imageName = tokens[2]
      reference.deploymentName = tokens[2]
    }
    if (tokensNumber > 3)
      reference.deploymentName = tokens[3]
    return reference
  }

  @NonCPS
  @Override
  String toString() {
    return [name, sourceName, imageName, deploymentName].join(' : ')
  }
}
