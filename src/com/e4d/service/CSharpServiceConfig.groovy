package com.e4d.service

import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput
import com.cloudbees.groovy.cps.NonCPS

class CSharpServiceConfig {
  ServiceReference service = new ServiceReference()
  String buildConfiguration = 'Debug'
  String projectPattern = /.*\.csproj/
  String unitTestProjectPattern = /.*\.([Tt]est|[Uu]nit[Tt]est)(s?).*\.csproj/
  String testFilter = ''
  String configmapPatch = ''
  String secretPatch = ''

  @NonCPS
  void setService(ServiceReference value) {
    service = value
  }

  void setService(String value) {
    service = ServiceReference.fromText(value)
  }

  void setConfigmapPatch(String value) {
    configmapPatch = value.stripIndent().trim()
  }

  def getConfigmapPatch() {
    parsedPatch(configmapPatch)
  }

  void setSecretPatch(String value) {
    secretPatch = value.stripIndent().trim()
  }  

  def getSecretPatch() {
    parsedPatch(secretPatch)
  }

  def defineParameters(pipeline) {
    [
      pipeline.string(name: 'SERVICE', defaultValue: "$service",
          description: 'Reference to a service in form of column separated ' +
              'string \'service-name : source-name : image-name\''),
      pipeline.string(name: 'SERVICE_PROJECT_PATTERN',
          defaultValue: projectPattern,
          description: 'Regex pattern defines names of projects to build'),
      pipeline.choice(name: 'SERVICE_BUILD_CONFIG', defaultValue: 'Debug',
          choices: ['Debug', 'Release'].join('\n'),
          description: 'Build configuration'),
      pipeline.string(name: 'SERVICE_UNIT_TEST_PROJECT_PATTERN',
          defaultValue: unitTestProjectPattern,
          description: 'Regex pattern defines names of projects contain unit tests'),
      pipeline.string(name: 'SERVICE_TEST_FILTER', defaultValue: testFilter,
          description: 'Pattern in msbuild syntax selects tests to be run'),
      pipeline.text(name: 'SERVICE_CONFIGMAP_PATCH', defaultValue: configmapPatch,
          description: 'Patch for the service\'s configmap'),
      pipeline.text(name: 'SERVICE_SECRET_PATCH', defaultValue: secretPatch,
          description: 'Patch for the service\'s secret')
    ]
  }

  def loadParameters(params) {
    service = params.SERVICE ?: service
    buildConfiguration = params.SERVICE_BUILD_CONFIG ?: buildConfiguration
    projectPattern = params.SERVICE_PROJECT_PATTERN ?: projectPattern
    unitTestProjectPattern = params.SERVICE_UNIT_TEST_PROJECT_PATTERN ?: unitTestProjectPattern
    testFilter = params.SERVICE_TEST_FILTER?.trim() ?: testFilter
    configmapPatch = params.SERVICE_CONFIGMAP_PATCH ?: configmapPatch
    secretPatch = params.SERVICE_SECRET_PATCH ?: secretPatch
  }

  @NonCPS
  def parsedPatch(String patch) {
    def patchMap = new JsonSlurperClassic().parseText(
        patch.startsWith('{') ? patch : "{$patch}")
    patchMap.keySet().inject([:]) { map, key ->
      map += ["$key": JsonOutput.toJson(patchMap[key])]
    }
  }
}
