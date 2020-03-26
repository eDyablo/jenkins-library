package com.e4d.service

import com.e4d.build.VersionTag
import com.cloudbees.groovy.cps.NonCPS

class ServiceDeployment
{
  String name
  String image
  VersionTag versionTag
  String tag

  @NonCPS
  @Override
  String toString() {
    return "[name: '$name', image: $image, versionTag: $versionTag, tag: $tag]"
  }
}
