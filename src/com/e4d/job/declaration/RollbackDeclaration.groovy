package com.e4d.job.declaration

import com.e4d.declaration.Declaration
import com.e4d.job.Option
import com.e4d.job.PipelineJob

class RollbackDeclaration extends Declaration {
  final PipelineJob job

  RollbackDeclaration(PipelineJob job) {
    this.job = job
  }

  def getAlways() {
    job.options.rollback.when = Option.When.ALWAYS
  }

  def getOnFailure() {
    job.options.rollback.when = Option.When.ON_FAILURE
  }
}
