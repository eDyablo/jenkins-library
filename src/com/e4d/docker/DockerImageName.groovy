package com.e4d.docker

import com.e4d.build.VersionTag
import com.cloudbees.groovy.cps.NonCPS

class DockerImageName {
  String name
  VersionTag tag

  static DockerImageName fromText(String text) {
    if (text == null)
      return null
    DockerImageName imageName = new DockerImageName()
    def tokens = text.tokenize(':')
    imageName.name = tokens[0]
    if (tokens.size() > 1)
      imageName.tag = VersionTag.fromText(tokens[1])
    return imageName
  }

  @NonCPS
  @Override  
  String toString() {
    return [name, tag].join(':')
  }
}
