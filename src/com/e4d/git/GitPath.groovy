package com.e4d.git

import com.cloudbees.groovy.cps.NonCPS

class GitPath {
  String repository
  String path

  @NonCPS
  static GitPath fromText(String text) {
    if (text == null)
      return null
    String[] components = text.tokenize('/')
    return new GitPath(
      repository: components[0],
      path: components.drop(1).join('/'))
  }

  String fullPath() {
    return [repository, path].join('/')
  }

  @NonCPS
  @Override
  String toString() {
    return [repository, path].join('/')
  }
}
