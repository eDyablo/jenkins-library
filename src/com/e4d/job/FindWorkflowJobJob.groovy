package com.e4d.job

import hudson.model.StringParameterDefinition
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * The job goes through all Jenkins workflow jobs and collects those that
 * match specific name pattern and their script contains text matches
 * the specified pattern.
 */
class FindWorkflowJobJob extends MaintenanceJob {  
  /**
   * Regular expression pattern for job full name.
   */
  String fullNameRegex = ''
  
  /**
   * Regular expression pattern for job script.
   */
  String scriptRegex = ''

  FindWorkflowJobJob(workflow) {
    super(workflow)
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

  String getWorkflowScript() {
    '''
    e4d.findWorkflowJob {
    }
    '''
  }

  void loadParameters() {
    if (workflow.params['full name regex'] != null) {
      fullNameRegex = workflow.params['full name regex']
    }
    if (workflow.params['script regex'] != null) {
      scriptRegex = workflow.params['script regex']
    }
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
