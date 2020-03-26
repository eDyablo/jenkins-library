package com.e4d.job

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.model.Items
import hudson.model.ParametersDefinitionProperty
import hudson.model.StringParameterDefinition
import hudson.tasks.LogRotator
import jenkins.model.BuildDiscarderProperty
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

class BootstrapJob implements Job {
  final def workflow

  BootstrapJob(workflow) {
    this.workflow = workflow
  }

  Jenkins getJenkins() {
    Jenkins.get()
  }

  String getFullName() {
    workflow.env.JOB_NAME
  }

  def getParameterDefinitions() {
  }

  void loadParameters() {
  }

  void initialize() {
  }

  void run() {
    workflow.stage('bootstrap') {
      setupLobby()
    }
  }

  void setupLobby() {
    final lobby = jenkins.allItems(Folder).find {
      it.fullName == 'lobby'
    } as Folder ?: jenkins.createProject(Folder, 'lobby') as Folder

    final jobItem = lobby.allItems(WorkflowJob).find {
      it.name == 'setup-service'
    } as WorkflowJob ?: lobby.createProject(WorkflowJob, 'setup-service') as WorkflowJob

    final job = new SetupServiceJob(workflow)

    jobItem.removeProperty(ParametersDefinitionProperty)

    jobItem.addProperty(new ParametersDefinitionProperty(
      job.parameterDefinitions))

    jobItem.addProperty(new BuildDiscarderProperty(
      new LogRotator('', '1', '', '')))

    jobItem.definition = new CpsFlowDefinition('''\
      e4d.setupService {
      }
    '''.stripIndent(), true)

    jobItem.save()
  }
}