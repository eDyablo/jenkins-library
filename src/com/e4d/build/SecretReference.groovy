package com.e4d.build

import com.cloudbees.groovy.cps.NonCPS

class SecretReference {
  String name
  String key

  static final String delimiter = ':'

  @NonCPS
  static SecretReference fromText(String text) {
    def tokens = text?.tokenize(delimiter) ?: []
    tokens = tokens.collect{ it.trim() }.findAll{ it }
    def reference = new SecretReference(name: tokens[0], key: tokens[0])
    def tokensNumber = tokens.size()
    if (tokensNumber > 1)
      reference.key = tokens[1]
    return reference
  }

  @NonCPS
  @Override
  String toString() {
    [name, key].join(delimiter)
  }

  String pretty() {
    [name, key].findAll{ it }.join(" ${ delimiter } ")
  }

  boolean equals(other) {
    if (other instanceof SecretReference) {
      name == other.name && key == other.key
    }
  }
}
