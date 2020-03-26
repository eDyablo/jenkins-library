package com.e4d.nexus

import groovy.json.JsonSlurperClassic

class NexusClient {
  final pipeline
  final String baseUrl
  final String user
  final String password

  NexusClient(pipeline, String baseUrl, String user, String password) {
    this.pipeline = pipeline
    this.baseUrl = baseUrl
    this.user = user
    this.password = password
  }

  NexusResponse getResource(Map kwargs, String url) {
    request(kwargs, url, [request: 'GET'])
  }

  NexusResponse request(Map kwargs, String url, Map options = [:]) {
    pipeline.withEnv(["creds=$user:$password"]) {
      final String request = buildCurlRequest(kwargs, url, options)
      final String response = pipeline.sh(
        script: """\
          #!/usr/bin/env bash
          ${ request }
        """.stripIndent(),
        returnStdout: true,
        encoding: 'utf-8').trim()
      final parsed = new JsonSlurperClassic().parseText(response ?: '{}')
      return new NexusResponse(parsed)
    }
  }

  String buildCurlRequest(Map kwargs, String url, Map options = [:]) {
    final args = kwargs.collect { "$it.key=$it.value" }
    final argsText = args ? "?${args.join('&')}" : ''
    final String requestUrl = "https://$baseUrl/service/rest/$url$argsText"
    options += [
      header: 'Accept: application/json',
      user: '${creds}',
    ]
    final optionsText = options.collect { "--$it.key \"$it.value\"" }.join(' ')
    return "curl $optionsText \"$requestUrl\""
  }

  NexusAsset[] searchAssets(Map kwargs) {
    def args = kwargs
    def assets = []
    def response = getResource(args, 'v1/search/assets')
    response.data.items.each {
      assets << new NexusAsset(
        id: it.id,
        downloadUrl: it.downloadUrl,
        path: it.path)
    }
    while (response.data.continuationToken) {
      args.continuationToken = response.data.continuationToken
      response = getResource(args, 'v1/search/assets')
      response.data.items.each {
        assets << new NexusAsset(
          id: it.id,
          downloadUrl: it.downloadUrl,
          path: it.path)
      }
    }
    return assets as NexusAsset[]
  }

  NexusComponent[] searchComponents(Map kwargs) {
    def args = kwargs
    def components = []
    def response = getResource(args, 'v1/search')
    response.data.items.each {
      def assets = it.assets.collect { asset ->
        new NexusAsset(
          id: asset.id,
          path: asset.path,
          downloadUrl: asset.downloadUrl)
      }
      components << new NexusComponent(
        id: it.id,
        repository: it.repository,
        name: it.name,
        version: it.version,
        assets: assets)
    }
    while (response.data.continuationToken) {
      args.continuationToken = response.data.continuationToken
      response = getResource(args, 'v1/search')
      response.data.items.each {
        def assets = it.assets.collect { asset ->
          new NexusAsset(
            id: asset.id,
            path: asset.path,
            downloadUrl: asset.downloadUrl)
        }
        components << new NexusComponent(
          id: it.id,
          repository: it.repository,
          name: it.name,
          version: it.version,
          assets: assets)
      }
    }
    return components as NexusComponent[]
  }

  def deleteComponent(String id) {
    deleteComponents([id])
  }

  def deleteComponents(List<String> ids) {
    def shellCommands = ids.collect {
      def curlCommand = buildCurlRequest([:], "beta/components/$it", [request: 'DELETE'])
      "$curlCommand && echo $it has been deleted"
    }.join('\n')
    pipeline.sh("{\n$shellCommands\n} 2> /dev/null")
  }
}
