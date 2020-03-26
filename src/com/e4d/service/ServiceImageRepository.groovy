package com.e4d.service

import com.e4d.service.ServiceImage
import com.e4d.build.ListUtils
import com.e4d.build.VersionTag
import com.e4d.nexus.NexusAsset
import com.e4d.nexus.NexusClient
import com.e4d.nexus.NexusComponent

class ServiceImageRepository implements Serializable {
  NexusClient client

  static final String REPOSITORY = 'debug-container'

  static final Closure NON_PR_COMPONENT = { it.version.contains('-pr') == false }

  ServiceImageRepository(NexusClient client) {
    this.client = client
  }

  ServiceImage[] getAllImages() {
    NexusComponent[] components = client.searchComponents(repository: REPOSITORY)
    return components.findAll(NON_PR_COMPONENT)
        .collect { makeImage(it) }
  }

  ServiceImage getRecentImage(String serviceName) {
    ServiceImage[] images = getNonPullRequestImages(serviceName)
    return ListUtils.findMax(images, { it.versionTag })
  }

  ServiceImage getOldestImage(String serviceName) {
    ServiceImage[] images = getNonPullRequestImages(serviceName)
    return ListUtils.findMin(images, { it.versionTag })
  }

  ServiceImage[] getNonPullRequestImages(String serviceName) {
    NexusComponent[] components = client.searchComponents(
        repository: REPOSITORY, name: serviceName)
    return components.findAll(NON_PR_COMPONENT)
        .collect { makeImage(it) }
  }

  ServiceImage makeImage(NexusComponent component) {
    def tags = splitTags(component.version)
    return new ServiceImage(id: component.id,
        name: component.name, versionTag: VersionTag.fromText(tags.version),
        tag: tags.rest, downloadUrl: component.assets[0]?.downloadUrl)
  }

  def splitTags(String tags) {
    def parts = tags?.split(/[^\d.]+/) ?: []
    def version = parts[0] ?: ''
    def rest = tags?.substring(version?.length() ?: 0) ?: ''
    return [version: version, rest: rest]
  }

  def deleteImages(def images) {
    client.deleteComponents(images.collect { it.id })
  }
}
