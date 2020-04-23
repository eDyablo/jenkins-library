package com.e4d.declaration

import hudson.tasks.LogRotator
import jenkins.model.BuildDiscarderProperty
import org.jenkinsci.plugins.workflow.job.WorkflowJob

class DiscardJobBuildsDeclaration extends Declaration {
  final WorkflowJob job

  DiscardJobBuildsDeclaration(WorkflowJob job) {
    this.job = job
  }

  void rotate(Map options) {
    final rotator = new LogRotator(
      options.inDays?.toString() ?: '',
      options.afterAmount?.toString() ?: '',
      '', ''
    )
    final property = new BuildDiscarderProperty(rotator)
    job.removeProperty(BuildDiscarderProperty)
    job.addProperty(property)
  }
}
