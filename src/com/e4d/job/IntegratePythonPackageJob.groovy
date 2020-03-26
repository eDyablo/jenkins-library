package com.e4d.job

import com.e4d.git.GitConfig
import com.e4d.nexus.NexusConfig
import com.e4d.pip.PipConfig
import com.e4d.shell.Shell
import com.e4d.step.CheckoutRecentSourceStep

class IntegratePythonPackageJob extends PipelineJob {
  final GitConfig gitConfig = new GitConfig()
  final NexusConfig nexusConfig = new NexusConfig()
  final Shell shell
  PipConfig pipConfig = new PipConfig()
  String sourceRoot = ''

  final static String requirementFile = 'requirements.txt'
  final static String setupFile = 'setup.py'

  IntegratePythonPackageJob(pipeline, Shell shell) {
    super(pipeline)
    this.shell = shell
    gitConfig.with(DefaultValues.git)
    pipConfig.with(DefaultValues.pip)
    nexusConfig.with(DefaultValues.nexus)
  }

  void sourceRoot(String path) {
    this.sourceRoot = path
  }

  def defineEnvVars() {
    pipConfig.defineEnvVars(pipeline)
  }

  def defineVolumes() {
    pipConfig.defineVolumes(pipeline)
  }

  def run() {
    final def source = checkout()
    pipeline.dir(source.dir) {
      pipeline.dir(sourceRoot) {
        installRequirement()
        buildPackage()
        publishPackage()
      }
    }
  }

  def checkout() {
    stage('checkout') {
      new CheckoutRecentSourceStep(
        baseUrl: gitConfig.baseUrl,
        repository: gitConfig.repository,
        credsId: gitConfig.credsId,
        branch: pipeline.params.sha1 ?: gitConfig.branch,
      ).run()
    }
  }

  void installRequirement() {
    stage('install') {
      if (pipeline.fileExists(requirementFile)) {
        shell.execute(script: """\
          #!/usr/bin/env bash
          pip install --upgrade pip
          pip install --requirement "${ requirementFile }"
        """.stripIndent(), [])
      }
    }
  }

  void buildPackage() {
    stage('build') {
      if (pipeline.fileExists(setupFile)) {
        shell.execute(script: """\
          #!/usr/bin/env bash
          pip install setuptools wheel
          python "${ setupFile }" sdist bdist_wheel
        """.stripIndent(), [])
      }
    }
  }

  void publishPackage() {
    stage('publish') {
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
        shell.execute(script: """\
          #!/usr/bin/env bash
          set -o errexit
          pip install twine==2.0.0
          twine upload \
          --repository-url "${ pypiRepoUrl }/" \
          --username "${ pipeline.env.user }" \
          --password "${ pipeline.env.password }" \
          --disable-progress-bar \
          "dist/*.tar.gz"
        """.stripIndent(), [])
      }
    }
  }
}
