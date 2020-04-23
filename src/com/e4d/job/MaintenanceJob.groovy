package com.e4d.job

import jenkins.model.Jenkins

abstract class MaintenanceJob implements Job {
  final def workflow

  MaintenanceJob(workflow) {
    this.workflow = workflow
  }

  Jenkins getJenkins() {
    Jenkins.instanceOrNull
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
}
