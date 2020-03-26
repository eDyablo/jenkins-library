package com.e4d.k8s

import groovy.json.JsonOutput
import com.cloudbees.groovy.cps.NonCPS

class K8sResponse {
  def data
  
  K8sResponse(data) {
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
