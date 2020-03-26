package com.e4d.nexus

import com.cloudbees.groovy.cps.NonCPS

class NexusComponent {
  String id
  String repository
  String name
  String version
  NexusAsset[] assets

  @NonCPS
  @Override
  String toString() {
    return "[id: '${id}', repository: '${repository}', name: '${name}', " <<
        "version: '${version}', assets: [${assets.join(', ')}]]"
  }
}
