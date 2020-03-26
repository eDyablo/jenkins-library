package com.e4d.job

import com.e4d.build.SecretReference
import com.e4d.k8s.K8sConfigReference

/**
 * A set of default values uses by jobs.
 */
class DefaultValues {
  static final def nexus = {
    baseUrl = 'http://artifacts.k8s.us-west-2.dev.e4d.com'
    port = 8082
    credsId = 'e4d.nexus.ci'
    apiKey = '#nexus.api-key'
  }

  static final def k8sConfigSecret = {
    name = 'infra-secretconfigs'
    key = 'kube.config'
  }

  static final def git = {
    baseUrl = 'https://github.com/activehours'
    branch = 'develop'
    credsId = 'e4d-github-ci'
  }

  static final def k8s = {
    configRef = K8sConfigReference.fromText('infra-secretconfigs : kube.config')
  }

  static final def nuget = {
    configRef = SecretReference.fromText('infra-secretconfigs : nuget.config')
  }

  static final def pip = {
    configRef = SecretReference.fromText('infra-secretconfigs : pip.config')
  }
}
