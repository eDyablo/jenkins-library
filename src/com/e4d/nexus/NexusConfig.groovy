package com.e4d.nexus

import com.e4d.build.TextValue
import hudson.model.*
import com.cloudbees.groovy.cps.NonCPS

class NexusConfig {
  String baseUrl
  String credsId
  int port
  TextValue apiKey

  def defineParameters(def pipeline) {
    [
      new StringParameterDefinition('NEXUS_BASE_URL', baseUrl, 'Base URL to Nexus server that stores assets like docker images and nuget packages'),
      new StringParameterDefinition('NEXUS_CREDS_ID', credsId, 'Reference to Nexus credentials stored in Jenkins')
    ]
  }

  def loadParameters(def params) {
    baseUrl = params.NEXUS_BASE_URL ?: baseUrl
    credsId = params.NEXUS_CREDS_ID ?: credsId
  }

  @NonCPS
  void setApiKey(String value) {
    apiKey = new TextValue(value)
  }

  @NonCPS
  @Override
  String toString() {
    "[baseUrl: '${baseUrl}', credsId: '${credsId}']"
  }

  String getAuthorityName() {
    baseUrl - 'http://' - 'https://'
  }

  int getAuthorityPort() {
    port
  }

  String getAuthority() {
    [authorityName, port].join(':')
  }
}
