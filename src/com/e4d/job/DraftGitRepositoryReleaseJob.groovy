package com.e4d.job

import com.e4d.git.GitConfig
import com.e4d.git.GitSourceReference
import hudson.model.StringParameterDefinition
import hudson.model.TextParameterDefinition

class DraftGitRepositoryReleaseJob extends MaintenanceJob {
  GitConfig gitConfig = new GitConfig()
  GitSourceReference sourceRef = new GitSourceReference()
  String releaseTitle = ''
  String versionTag = ''
  def versionTags = []

  DraftGitRepositoryReleaseJob(workflow) {
    super(workflow)
    gitConfig.with(DefaultValues.git)
  }

  def getParameterDefinitions() {
    [
      new StringParameterDefinition(
        'source git', sourceRef?.toString(), '', true
      ),
      new StringParameterDefinition(
        'version tag', versionTag, '', true
      ),
      new StringParameterDefinition(
        'release title', releaseTitle, '', true
      ),
    ]
  }

  String getWorkflowScript() {
    '''
    e4d.draftGitRepositoryRelease {
    }
    '''
  }

  void loadParameters() {
    releaseTitle = workflow.params['release title'] ?: releaseTitle
    sourceRef = workflow.params['source git'] ? new GitSourceReference(workflow.params['source git']) : sourceRef
    versionTag = workflow.params['version tag'] ?: versionTag
  }

  void initialize() {
    sourceRef = new GitSourceReference(
      scheme: sourceRef.host
        ? sourceRef.scheme
        : (gitConfig.host ? gitConfig.protocol : sourceRef.scheme),
      host: sourceRef.host ?: gitConfig.host,
      owner: sourceRef.owner ?: gitConfig.owner,
      repository: sourceRef.repository,
      branch: sourceRef.branch ?: gitConfig.branch,
    )
    if (versionTag.startsWith('v') == false) {
      versionTag = "v${ versionTag }".toString()
    }
  }

  void run() {
    workflow.node {
      workflow.stage('checkout') {
        checkout()
      }
      workflow.stage('study') {
        versionTags = fetchAllVersionTags()
      }
      workflow.stage('draft') {
        if (versionTags.any { it == versionTag }) {
          workflow.unstable("the version tag '${ versionTag }' is already exist")
        } else {
          draftNewRelease()
        }
      }
    }
  }

  void checkout() {
    workflow.checkout(
      $class: 'GitSCM',
      branches: [
        [
          name: sourceRef.branch
        ],
      ],
      doGenerateSubmoduleConfigurations: false,
      extensions: [
        [$class: 'PruneStaleBranch'],
        [$class: 'WipeWorkspace'],
      ],
      submoduleCfg: [],
      userRemoteConfigs: [
        [
          credentialsId: gitConfig.credsId,
          url: sourceRef.repositoryUrl,
          refspec: '+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*',
        ]
      ]
    )
    workflow.sh("git checkout ${ sourceRef.branch }")
  }

  def fetchAllVersionTags() {
    workflow.sh(script: 'git tag --list \'v*.*.*\'', returnStdout: true).split('\n').findAll()
  }

  void draftNewRelease() {
    workflow.withCredentials([
      workflow.usernamePassword(
        credentialsId: gitConfig.credsId,
        usernameVariable: 'git_username',
        passwordVariable: 'git_password',
      )
    ]) {
      final repositoryUrl = sourceRef.repositoryUrl.replace(
        '://', '://${git_username}:${git_password}@')
      workflow.sh("""
        set -o errexit
        git commit --allow-empty --message '${ releaseTitle ?: versionTag }'
        git tag --annotate '${ versionTag }' --message '${ releaseTitle }'
        git push ${ repositoryUrl } ${ sourceRef.branch } --follow-tags
      """.stripIndent().trim())
    }
  }
}
