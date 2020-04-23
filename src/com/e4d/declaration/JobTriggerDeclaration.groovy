package com.e4d.declaration

import org.jenkinsci.plugins.workflow.job.WorkflowJob

class JobTriggerDeclaration extends Declaration {
  final WorkflowJob job

  JobTriggerDeclaration(WorkflowJob job) {
    this.job = job
  }

  def getGitHubPush() {
    GitHubDeclaration.newInstance(job).pushTrigger
  }
}
