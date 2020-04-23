package com.e4d.job

import com.e4d.declaration.JenkinsDeclaration

abstract class SetupJob extends MaintenanceJob {
  SetupJob(workflow) {
    super(workflow)
  }

  void jenkins(Closure definition) {
    definition.delegate = new JenkinsDeclaration()
    definition.resolveStrategy = Closure.DELEGATE_FIRST
    definition.call()
  }
}
