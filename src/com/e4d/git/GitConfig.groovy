package com.e4d.git

import com.cloudbees.groovy.cps.NonCPS

class GitConfig {
  String branch
  String credsId
  String _protocol
  String _host
  String _owner
  String _repository

  GitConfig() {
  }

  GitConfig(Map options) {
    _protocol = options.scheme
    _host = options.host
    _owner = options.owner
    _repository = options.repository
    branch = options.branch
    credsId = options.credsId
  }

  def defineParameters(def pipeline) {
    return [
      pipeline.string(name: 'GIT_BASE_URL', defaultValue: repositoryURL, description: 'URL to Git repository'),
      pipeline.string(name: 'GIT_CREDS_ID', defaultValue: credsId, description: 'Reference to Git credentials stored in Jenkins'),
      pipeline.string(name: 'GIT_BRANCH', defaultValue: branch, description: 'Name of the branch'),
    ]
  }

  def loadParameters(def params) {
    baseUrl = params.GIT_BASE_URL ?: repositoryURL
    branch = params.GIT_BRANCH ?: branch
    credsId = params.GIT_CREDS_ID ?: credsId
  }

  @NonCPS
  String getProtocol() {
    _protocol
  }

  @NonCPS
  void setProtocol(String value) {
    _protocol = value
  }

  @NonCPS
  String getHost() {
    _host
  }

  @NonCPS
  void setHost(String value) {
    _host = value
  }

  @NonCPS
  String getOwner() {
    _owner
  }

  @NonCPS
  void setOwner(String value) {
    _owner = value
  }

  @NonCPS
  String getRepository() {
    _repository
  }

  @NonCPS
  void setRepository(String value) {
    _repository = value
  }

  @NonCPS
  String getHostURL() {
    [protocol, host].findAll{ it }.join('://')
  }

  @NonCPS
  String getOwnerURL() {
    [hostURL, owner].findAll{ it }.join('/')
  }

  @NonCPS
  String getRepositoryURL() {
    [ownerURL, repository].findAll().join('/')
  }

  @NonCPS
  void setBaseUrl(String value) {
    final parts = value.split('://', 2)
    if (parts.size() > 1) {
      protocol = parts.first()
    }
    (host, owner, repository) = parts.last().split('/').findAll()
  }

  @NonCPS
  String getBaseUrl() {
    final String path = [host, owner].findAll().join('/')
    if (protocol) {
      [protocol, path].join('://')
    } else {
      path
    }
  }

  @NonCPS
  @Override
  String toString() {
    return "[repositoryUrl: '${repositoryURL}', branch: '${branch}', credsId: '${credsId}']"
  }
}
