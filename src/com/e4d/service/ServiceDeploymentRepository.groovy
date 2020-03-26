package com.e4d.service

import com.e4d.build.VersionTag
import com.e4d.service.ServiceDeployment
import com.e4d.k8s.K8sClient
import com.e4d.k8s.K8sResponse

class ServiceDeploymentRepository {
  K8sClient client

  ServiceDeploymentRepository(K8sClient client) {
    this.client = client
  }

  def withContext(String context, Closure action) {
    action.delegate = this
    def previous = client.useContext(context)
    try {
      action()
    }
    finally {
      client.useContext(previous)
    }
  }

  def getRecentDeployment(String name) {
    def response = client.getDeployment(name)
    return makeDeployment(response.data)
  }

  def getAllDeployments() {
    def response = client.getDeployments(K8sClient.ALL_NAMESPACES)
    return response.data.items.collect { makeDeployment(it) }
  }

  def makeDeployment(def data) {
    def image = data.spec?.template?.spec?.containers?.getAt(0)?.image
    def tags = splitTags(extractImageTag(image))
    return new ServiceDeployment(name: data.metadata?.name,
        image: image, versionTag: VersionTag.fromText(tags.version),
        tag: tags.rest)
  }

  def extractImageTag(String image) {
    def matches = (image =~ /.*:(\S*)/)
    return matches ? matches[0][1] : null
  }

  def splitTags(String tags) {
    if (tags =~ /^v?\s*\d/) {
      tags -= 'v'
    }
    tags = tags?.trim()
    def parts = tags?.split(/[^\d.]+/) ?: []
    def version = parts[0] ?: ''
    def rest = tags?.substring(version?.length() ?: 0) ?: ''
    rest -= '-'
    return [version: version, rest: rest.trim()]
  }
}
