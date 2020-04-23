package com.e4d.declaration

import com.cloudbees.jenkins.GitHubPushTrigger
import com.coravy.hudson.plugins.github.GithubProjectProperty
import org.jenkinsci.plugins.ghprb.extensions.GhprbExtension
import org.jenkinsci.plugins.ghprb.GhprbBranch
import org.jenkinsci.plugins.ghprb.GhprbTrigger
import org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty
import org.jenkinsci.plugins.workflow.job.WorkflowJob

class GitHubDeclaration extends Declaration {
  final WorkflowJob job

  GitHubDeclaration(WorkflowJob job) {
    this.job = job
  }

  void project(String projectUrl) {
    final property = GithubProjectProperty.newInstance(projectUrl)
    job.removeProperty(GithubProjectProperty)
    job.addProperty(property)
  }

  void pullRequestBuilder(Map options) {
    final trigger = new GhprbTrigger(
      options.adminList?.join('\n') ?: '',
      options.whiteList?.join('\n') ?: '',
      options.orgsList?.join('\n') ?: '',
      options.cron ?: '',
      options.triggerPhrase ?: '',
      options.onlyTriggerPhrase ?: false,
      options.useGitHubHooks ?: true,
      options.permitAll ?: false,
      options.autoCloseFailedPullRequests ?: false,
      options.displayBuildErrorsOnDownstreamBuilds ?: false,
      options.commentFilePath ?: '',
      options.skipBuildPhrase ?: /.*\[[Ss]kip\W+(ci|CI)\].*/,
      options.blackListCommitAuthor ?: '',
      (options.whiteListTargetBranches ?: []) as List<GhprbBranch>,
      (options.blackListTargetBranches ?: []) as List<GhprbBranch>,
      options.allowMembersOfWhitelistedOrgsAsAdmin ?: false,
      options.msgSuccess ?: '',
      options.msgFailure ?: '',
      options.commitStatusContext ?: '',
      options.gitHubAuthId ?: '',
      options.buildDescTemplate ?: '',
      options.blackListLabels?.join('\n') ?: '',
      options.whiteListLabels?.join('\n') ?: '',
      (options.extensions ?: []) as List<GhprbExtension>,
      options.includedRegions?.join('\n') ?: '',
      options.excludedRegions?.join('\n') ?: '',
    )
    final property = new PipelineTriggersJobProperty([trigger])
    job.removeProperty(PipelineTriggersJobProperty)
    job.addProperty(property)
  }

  def getPushTrigger() {
    final trigger = new GitHubPushTrigger()
    final property = new PipelineTriggersJobProperty([trigger])
    job.removeProperty(PipelineTriggersJobProperty)
    job.addProperty(property)
  }

  void pullRequestBuilder(Closure definition) {
    final declaration = GitHubPullRequestBuilderDeclaration.newInstance()
    define(declaration, definition)
    pullRequestBuilder(declaration.options)
  }
}
