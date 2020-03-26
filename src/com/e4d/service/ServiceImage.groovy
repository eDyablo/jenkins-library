package com.e4d.service

import com.e4d.build.VersionTag
import com.cloudbees.groovy.cps.NonCPS

class ServiceImage {
  String name
  String downloadUrl
  VersionTag versionTag
  String tag
  String id

  @NonCPS
  @Override
  String toString() {
    return "[name: '$name', downloadUrl: $downloadUrl, " <<
        "versionTag: $versionTag, tag: $tag]"
  }
}
