package com.e4d.template

import com.cloudbees.groovy.cps.NonCPS

class FilesCollector {
  final Set files = []

  @NonCPS
  def read(String name) {
    files.add(name)
    return name
  }

  @NonCPS
  def getJson() {
    new JsonFilesCollector(this)
  }
}
