package com.e4d.template

import com.cloudbees.groovy.cps.NonCPS

class FilesContentReader {
  final Set files = []
  final Map contents = [:]

  String encoding = 'utf-8'

  FilesContentReader(files) {
    this.files = files
  }

  def read(pipeline) {
    files.each {
      contents[it] = pipeline.readFile(file: it, encoding: encoding)
    }
  }

  @NonCPS
  def read(String name) {
    contents[name].split('\n')*.trim().join(' ')
  }

  @NonCPS
  def getJson() {
    new JsonFileReader(this)
  }
}
