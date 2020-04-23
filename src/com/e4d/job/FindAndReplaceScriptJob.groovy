package com.e4d.job

import hudson.model.BooleanParameterDefinition
import hudson.model.StringParameterDefinition
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * The job goes through all Jenkins workflow jobs and collects those that
 * match specific name pattern and their script contains text matches
 * the specified pattern.
 * In found jobs it substitutes part of their scripts that maches the pattern.
 */
class FindAndReplaceScriptJob extends MaintenanceJob {
  /**
   * Regular expression pattern for job full name.
   */
  String fullNameRegex = ''

  /**
   * Regular expression pattern for job script.
   */
  String scriptRegex = ''

  /**
   * Text that will replace part of original script found by the pattern.
   */
  String scriptReplacement = ''

  /**
   * Switch that prevents unintended modification of a script.
   */
  Boolean dryRun = true

  FindAndReplaceScriptJob(workflow) {
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
      new StringParameterDefinition(
        'script replacement', scriptReplacement, 'text to be placed instead of found', true
      ),
      new BooleanParameterDefinition(
        'dry run', true, 'do not apply a change when it is set'
      ),
    ]
  }

  String getWorkflowScript() {
    '''
    e4d.findAndReplaceScript {
    }
    '''
  }

  void loadParameters() {
    if (workflow.params.'full name regex' != null) {
      fullNameRegex = workflow.params.'full name regex'
    }
    if (workflow.params.'script regex' != null) {
      scriptRegex = workflow.params.'script regex'
    }
    if (workflow.params.'script replacement' != null) {
      scriptReplacement = workflow.params.'script replacement'
    }
    if (workflow.params.'dry run' != null) {
      dryRun = workflow.params.'dry run'
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
      }.collect(report) { job ->
        final patchedScript = job.definition.script.replaceAll(
          scriptRegex, scriptReplacement)
        final record = [
          [jenkins.rootUrl, job.url].join(''),
          'original script:',
          job.definition.script,
          'patched script:',
          patchedScript,
        ].join('\n')
        if (!dryRun) {
          job.definition = new CpsFlowDefinition(patchedScript, true)
          job.save()
        }
        record
      }
      log(report)
    }
  }
  
  def echo(String message) {
    workflow.echo message
  }

  def log(String message) {
    echo(message)
  }

  def log(List records) {
    log(records.collect {
      it.toString()
    }.join('\n'))
  }
}
