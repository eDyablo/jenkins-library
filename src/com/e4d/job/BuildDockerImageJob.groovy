package com.e4d.job

import com.e4d.build.*
import com.e4d.docker.*
import com.e4d.git.*
import com.e4d.nexus.*

class BuildDockerImageJob extends PipelineJob {
  GitConfig git
  NexusConfig nexus
  String dockerfilePath
  String gitRepositoryName
  String dockerImageName

  BuildDockerImageJob(def pipeline) {
    super(pipeline)
    git = new GitConfig(credsId: pipeline.GLOBAL_GITSCM_CREDSID)
    nexus = new NexusConfig()
  }

  def defineParameters() {
    return git.defineParameters(pipeline)
      .plus(nexus.defineParameters(pipeline))
      .plus([
        pipeline.string(name: 'GIT_REPOSITORY_NAME', defaultValue: gitRepositoryName,  description: 'git repo name in github project'),        
        pipeline.string(name: 'DOCKERFILE_PATH',     defaultValue: dockerfilePath,     description: 'Path to Dockerfile from repository root'),
        pipeline.string(name: 'DOCKERIMAGE_NAME',    defaultValue: dockerImageName,    description: 'built docker image name to publish'),        
      ])
  }

  def loadParameters(def params) {
    git.loadParameters(params)
    nexus.loadParameters(params)
    gitRepositoryName = params.GIT_REPOSITORY_NAME    
    dockerfilePath    = params.DOCKERFILE_PATH
    dockerImageName   = params.DOCKERIMAGE_NAME    
  }

  def run() {
    def sourceCode = pipeline.checkoutRecentSource(
      repository: gitRepositoryName,
      baseUrl: git.baseUrl,
      credsId: git.credsId,
      branch: git.branch
    )
    pipeline.stage('build & push') {
      def nexusRepoShortUrl = nexus.baseUrl - 'http://'
      def tag = "${nexusRepoShortUrl}:8082/${dockerImageName}"
      def docker = new DockerClient(pipeline)
      
      pipeline.withCredentials([pipeline.usernamePassword(
          credentialsId: nexus.credsId, usernameVariable: 'user',
          passwordVariable: 'password')]) {
        pipeline.dir(sourceCode.dir) {
          docker.buildImage(dockerfilePath, tag, [
            nexus_url: nexusRepoShortUrl,
            nexus_user: pipeline.env.user,
            nexus_password: pipeline.env.password
            ])
        }
        docker.pushImage(tag, "${nexus.baseUrl}:8082",
            pipeline.env.user, pipeline.env.password)
      }
      def gitClient = new GitClient(pipeline, sourceCode)
      def gitSCMTag = DockerImageName.fromText(dockerImageName).tag
      gitClient.setTag("${gitSCMTag}")
    }
  }
}
