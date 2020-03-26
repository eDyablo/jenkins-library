package com.e4d.job

import com.cloudbees.groovy.cps.NonCPS
import com.e4d.build.*
import com.e4d.git.*
import com.e4d.nexus.*
import com.e4d.pip.*
import com.e4d.service.*
import com.e4d.step.*

class HttpClientGeneratorJob extends IntegrateJob {
  NexusConfig nexusConfig = new NexusConfig()

  // TODO: Add C# Nuget upload support.
  PipConfig pipConfig = new PipConfig()

  final def generatorJar = '/opt/openapi-generator-cli.jar'
  Map values = [:]

  HttpClientGeneratorJob(pipeline) {
    super(pipeline)
    nexusConfig.with(DefaultValues.nexus)
    pipConfig.with(DefaultValues.pip)
  }

  @NonCPS
  void setSourceGit(String text) {
    gitSourceRef = new GitSourceReference(text)
  }

  def run() {
    stage('checkout') {
      checkout()
    }
    stage('generate http client') {
      generateHttpClient()
    }
    stage('publish http client') {
      publishHttpClient()
    }
  }

  def generateHttpClient() {
    final def outputDir = "${ source.dir }/httpClient"
    def swaggerPath = pipeline.sh(script:"find ${ source.dir }/src -type f -name swagger.json", returnStdout: true)
    swaggerPath = swaggerPath.trim()

    // Error out if no swagger path is found
    if (!swaggerPath) {
      pipeline.error("Swagger path not found.")
    }

    def script = "java -jar ${ generatorJar } generate -g ${ values.language } -i ${ swaggerPath } -o ${ outputDir }"

    // Add additional language-specific properties.
    switch(values.language) {
      // For Python, we need a package name, a project name, and version.
      case 'python':

        // Package name is the repository name without the svc- in front, in snake case.
        def packageName = gitSourceRef.repository.tokenize('-')[(1..-1)].join('_')
        packageName += '_api_client'

        // Project name is the kebab case version of packageName (_ replaced with -)
        def projectName = packageName.replace('_', '-')

        def version = artifactVersion

        // Package name is snake cased, project name in setup.py is kebab cased
        script += " --additional-properties packageName=${ packageName },projectName=${ projectName },packageVersion=${ version }"
        break
      default:
        pipeline.error("Unsupported language ${ values.language }")
    }
    pipeline.sh(script:script)
  }

  def publishHttpClient() {
    switch(values.language) {
      case 'python':
        pythonPublishHttpClient()
        break
      default:
        pipeline.error("Unsupported language ${ values.language }")
    }
  }

  def pythonPublishHttpClient() {
    pipeline.dir("${ source.dir }/httpClient") {
      // Build the API client
      pipeline.sh(script: """\
        #!/usr/bin/env bash
        pip install setuptools wheel
        python setup.py sdist bdist_wheel
        """.stripIndent())

      // Publish the AOU client
      final def pypiRepoUrl = [
          'https:/',
          nexusConfig.authorityName,
          'repository', 'debug-pypi'
        ].join('/')
      final def creds = [pipeline.usernamePassword(
        credentialsId: nexusConfig.credsId,
        usernameVariable: 'user',
        passwordVariable: 'password')]
      pipeline.withCredentials(creds) {
        pipeline.sh(script: """\
          #!/usr/bin/env bash
          set -o errexit
          pip install twine==2.0.0
          twine upload \
          --repository-url "${ pypiRepoUrl }/" \
          --username "${ pipeline.env.user }" \
          --password "${ pipeline.env.password }" \
          --disable-progress-bar \
          "dist/*.tar.gz"
        """.stripIndent())
      }
    }
  }
}
