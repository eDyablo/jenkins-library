package com.e4d.template

import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput
import com.cloudbees.groovy.cps.NonCPS

class JsonFileReader {
  final FilesContentReader filesReader
  final MapMerger merger = new MapMerger()

  JsonFileReader(FilesContentReader reader) {
    filesReader = reader
  }

  @NonCPS
  def read(String name) {
    filesReader.read(name)
  }

  @NonCPS
  def merge(String[] files) {
    def documents = parse(files)
    def merged = merger.merge(documents)
    JsonOutput.toJson(merged)
  }

  @NonCPS
  def parse(String[] files) {
    def slurper = new JsonSlurperClassic()
    files.collect {
      slurper.parseText(filesReader.read(it))
    }
  }
}
