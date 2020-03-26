package com.e4d.template

import com.cloudbees.groovy.cps.NonCPS

class JsonFilesCollector {
  final FilesCollector files

  JsonFilesCollector(FilesCollector collector) {
    files = collector
  }

  @NonCPS
  def read(String name) {
    files.read(name)
  }

  @NonCPS
  def merge(String[] names) {
    names.each {
      read(it)
    }
  }
}
