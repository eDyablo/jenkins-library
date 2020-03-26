package com.e4d.build

import com.cloudbees.groovy.cps.NonCPS

class ArtifactReference {
  String name
  String tag

  static final String delimiter = ':'

  ArtifactReference(String name, String tag='') {
    this.name = name
    this.tag = tag
  }

  @NonCPS
  static ArtifactReference fromText(String text) {
    def tokens = text?.tokenize(delimiter) ?: []
    return new ArtifactReference(tokens[0].trim(),
      tokens.drop(1).join(delimiter).trim())
  }

  @Override
  @NonCPS
  String toString() {
    [name, tag].findAll{it}.join(delimiter)
  }
}
