package com.e4d.job

import com.e4d.docker.DockerTool
import com.e4d.nexus.NexusConfig
import com.e4d.nuget.NugetRepository
import com.e4d.pipeline.PipelineShell
import com.e4d.pipeline.PipelineTextValueResolver
import java.net.URI

class IntegrateDockerImageJob extends IntegrateJob {
  DockerTool docker
  NexusConfig nexusConfig = new NexusConfig()
  URI dockerRegistry

  IntegrateDockerImageJob(pipeline) {
    super(pipeline)
    nexusConfig.with(DefaultValues.nexus)
    nexusConfig.apiKey.resolver = new PipelineTextValueResolver(pipeline)
    docker = new DockerTool(new PipelineShell(pipeline))
  }

  void initializeJob() {
    super.initializeJob()
    if (!dockerRegistry) {
      dockerRegistry = new URI(nexusConfig.authority)
      dockerRegistry.userInfo = getUsernamePassword(nexusConfig.credsId).join(':')
    }
  }

  void run() {
    stage('checkout', gitSourceRef.repository) {
      checkout()
    }
    stage('study', source) {
      source += study()
    }
    stage('build', source?.dockerfile) {
      build()
    }
  }

  def study() {
    final dockerfile = [source.dir, 'Dockerfile'].join('/')
    return [
      dockerfile: pipeline.fileExists(dockerfile) ? dockerfile : null
    ].findAll { key, value -> value }
  }

  def build() {
    final def (String username, String password) =
      (dockerRegistry?.userInfo?.split(':') ?: []) + [null]
    docker.login(
      server: dockerRegistry.toString(),
      username: username,
      password: password,
    )
    docker.build(
      path: source.dir,
      network: 'host',
    )
  }
}
