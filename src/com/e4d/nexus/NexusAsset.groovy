package com.e4d.nexus

import com.cloudbees.groovy.cps.NonCPS

class NexusAsset {
  String id
  String downloadUrl
  String path
  final def checksum = [
    sha1: null,
    sha512: null,
  ]

  String getName() {
    path?.split('/', 2)?.take(1)?.findAll()?.getAt(0)?.trim()
  }

  String getVersion() {
    path?.split('/', 2)?.drop(1)?.findAll()?.getAt(0)?.trim()
  }

  @NonCPS
  @Override
  String toString() {
    return "[id: '${ id }', path: '${ path }', downloadUrl: '${ downloadUrl }']"
  }
}
