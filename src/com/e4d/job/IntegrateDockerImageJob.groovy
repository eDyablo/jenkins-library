package com.e4d.job

import com.cloudbees.groovy.cps.NonCPS
import com.e4d.docker.DockerTool
import com.e4d.nexus.NexusConfig
import com.e4d.nuget.NugetRepository
import com.e4d.pipeline.PipelineShell
import com.e4d.pipeline.PipelineTextValueResolver
import java.net.URI

class IntegrateDockerImageJob extends IntegrateJob {
  DockerTool docker
  NexusConfig nexusConfig = new NexusConfig()
  String imageId
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
      imageId = build()
    }
    stage('deliver', canDeliver) {
      deliver()
    }
  }

  def study() {
    final dockerfile = [source.dir, 'Dockerfile'].join('/')
    return [
      dockerfile: pipeline.fileExists(dockerfile) ? dockerfile : null
    ].findAll { key, value -> value }
  }

  def build() {
    final def (String username, String password) = dockerRegistryCreds
    docker.build(
      path: source.dir,
      network: 'host',
      registry: dockerRegistry,
      username: username,
      password: password,
    )
  }

  void deliver() {
    final def (String username, String password) = dockerRegistryCreds
    docker.push(imageId,
      name: targetImageName,
      registry: dockerRegistry,
      username: username,
      password: password,
    )
  }

  private def getDockerRegistryCreds() {
    (dockerRegistry?.userInfo?.split(':') ?: []) + [null]
  }

  private String getTargetImageName() {
    [artifactBaseName ?: gitSourceRef.repository, targetImageTag].join(':')
  }

  private String getTargetImageTag() {
    artifactVersion.toString().replace('+', '-')
  }

  boolean getCanDeliver() {
    imageId && (publishPrereleaseVersion || (!publishPrereleaseVersion && !artifactVersion.prerelease))
  }
}
