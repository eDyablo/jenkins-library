package com.e4d.job

import com.e4d.slack.SlackAppClient

class SlackPublisherDecl extends JobDeclaration {
  String urlReference

  SlackPublisherDecl(script, String urlReference) {
    super(script)
    this.urlReference = urlReference
  }

  def url() {
    if (urlReference.startsWith('#')) {
      script.withCredentials([script.string(credentialsId: urlReference[1..-1],
          variable: 'url')]) {
        return script.env.url
      }
    }
    else {
      return urlReference
    }
  }

  def createPublisher(Map options) {
    return new SlackAppClient(url())
  }
}
