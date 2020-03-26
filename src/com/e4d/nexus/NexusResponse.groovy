package com.e4d.nexus

import groovy.json.JsonOutput
import com.cloudbees.groovy.cps.NonCPS

class NexusResponse {
  def data
  
  NexusResponse(data) {
    this.data = data
  }
  
  @NonCPS
  @Override
  String toString() {
    return JsonOutput.toJson(data)
  }
  
  String pretty() {
    return JsonOutput.prettyPrint(toString())
  }
}
