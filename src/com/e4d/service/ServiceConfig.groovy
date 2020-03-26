package com.e4d.service

import com.cloudbees.groovy.cps.NonCPS

class ServiceConfig {
  ServiceReference serviceRef = new ServiceReference()

  @NonCPS
  ServiceReference getService() {
    serviceRef
  }

  @NonCPS
  void setService(ServiceReference value) {
    serviceRef = value
  }

  @NonCPS
  void setService(String value) {
    serviceRef = ServiceReference.fromText(value)
  }

  def defineParameters(pipeline) {
    [
      pipeline.string(name: 'SERVICE', defaultValue: serviceRef.toString(),
          description: 'Reference to a service in form of column separated ' +
              'string \'service-name : source-name : image-name\'')
    ]
  }

  def loadParameters(params) {
    service = params.SERVICE ?: service
  }
}
