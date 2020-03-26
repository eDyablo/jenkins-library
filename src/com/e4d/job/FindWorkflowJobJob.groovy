package com.e4d.job

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.model.StringParameterDefinition
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * The job goes through all Jenkins workflow jobs and collects those that
 * match specific name pattern and their script contains text matches
 * the specified pattern.
 */
class FindWorkflowJobJob implements Job {
  private def workflow
  
  /**
   * Regular expression pattern for job full name.
   */
  String fullNameRegex = ''
  
  /**
   * Regular expression pattern for job script.
   */
  String scriptRegex = ''

  private Jenkins getJenkins() {
    Jenkins.instanceOrNull
  }

  String getFullName() {
    workflow.env.JOB_NAME
  }

  def getParameterDefinitions() {
    [
      new StringParameterDefinition(
        'full name regex', fullNameRegex, 'regular expression pattern for job full name', true
      ),
      new StringParameterDefinition(
        'script regex', scriptRegex, 'regular expression pattern for job script', true
      ),
    ]
  }

  void loadParameters() {
    if (workflow.params['full name regex'] != null) {
      fullNameRegex = workflow.params['full name regex']
    }
    if (workflow.params['script regex'] != null) {
      scriptRegex = workflow.params['script regex']
    }
  }

  void initialize() {
  }

  void run() {
    workflow.stage('search') {
      final report = [
        'found jobs:'
      ]
      jenkins?.allItems(WorkflowJob).findAll { job ->
        job.fullName =~ fullNameRegex
      }.findAll { job ->
        job.definition instanceof CpsFlowDefinition 
      }.findAll { job ->
        job.definition.script =~ scriptRegex
      }.collect(report) { job ->
        [jenkins.rootUrl, job.url].join('')
      }
      log(report)
    }
  }

  private void echo(String message) {
    workflow.echo message
  }

  private void log(String message) {
    echo(message)
  }

  private void log(List records) {
    log(records.collect {
      it.toString()
    }.join('\n'))
  }
}
