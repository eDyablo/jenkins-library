package com.e4d.service

import com.e4d.build.ListUtils
import com.e4d.build.VersionTag
import com.e4d.git.GitClient

class ServiceSourceRepository {
  GitClient client

  ServiceSourceRepository(GitClient client) {
    this.client = client
  }

  def getVersionTags() {
    def textualTags = client.getTags()
    def numericalTags = textualTags.findAll{ it =~ /^(\d+\.?)+$/ }
    return numericalTags.collect{ VersionTag.fromText(it) }
  }

  def getRecentTag() {
    def tags = getVersionTags()
    return ListUtils.findMax(tags)
  }

  def getOldestTag() {
    def tags = getVersionTags()
    return ListUtils.findMin(tags)
  }
}
