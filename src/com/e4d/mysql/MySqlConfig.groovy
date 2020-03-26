package com.e4d.mysql

import com.cloudbees.groovy.cps.NonCPS

class MySqlConfig {
  String hostUrl
  String credsId

  def defineParameters(def pipeline) {
    return [
      pipeline.string(name: 'MYSQL_HOST_URL', defaultValue: hostUrl, description: 'URL to MySQL server'),
      pipeline.string(name: 'MYSQL_CREDS_ID', defaultValue: credsId, description: 'Reference to MySQL credentials stored in Jenkins'),
    ]
  }

  def loadParameters(def params) {
    hostUrl = params.MYSQL_HOST_URL
    credsId = params.MYSQL_CREDS_ID
  }

  @NonCPS
  @Override
  String toString() {
    return "[hostUrl: '${hostUrl}', credsId: '${credsId}']"
  }
}
