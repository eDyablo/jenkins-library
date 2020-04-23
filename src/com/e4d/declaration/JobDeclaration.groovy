package com.e4d.declaration

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.model.ParameterDefinition
import hudson.model.ParametersDefinitionProperty
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.properties.DisableConcurrentBuildsJobProperty
import org.jenkinsci.plugins.workflow.job.WorkflowJob

class JobDeclaration extends AbstractItemDeclaration {
  final WorkflowJob job

  JobDeclaration(WorkflowJob job) {
    super(job)
    this.job = job
  }

  void script(String text) {
    job.definition = CpsFlowDefinition.newInstance(text.stripIndent().trim(), true)
  }

  void discardBuilds(Closure definition) {
    define(DiscardJobBuildsDeclaration.newInstance(job), definition)
  }

  void discardBuilds(Map options) {
    DiscardJobBuildsDeclaration.newInstance(job).rotate(options)
  }

  void gitHub(Closure definition) {
    define(GitHubDeclaration.newInstance(job), definition)
  }

  void trigger(Closure definition) {
    define(JobTriggerDeclaration.newInstance(job), definition)
  }

  void parameters(List<ParameterDefinition> definitions) {
    job.removeProperty(ParametersDefinitionProperty)
    job.addProperty(new ParametersDefinitionProperty(definitions))
  }

  def getDisableConcurrentBuilds() {
    job.addProperty(new DisableConcurrentBuildsJobProperty())
  }

  def getEnableConcurrentBuilds() {
    job.removeProperty(DisableConcurrentBuildsJobProperty)
  }
}
