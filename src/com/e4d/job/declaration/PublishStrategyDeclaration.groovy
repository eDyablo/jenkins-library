package com.e4d.job.declaration

import com.e4d.declaration.Declaration
import com.e4d.job.IntegrateJob

class PublishStrategyDeclaration extends Declaration {
  final IntegrateJob job

  PublishStrategyDeclaration(IntegrateJob job) {
    this.job = job
  }

  def getSkipPrereleaseVersion() {
    job.publishPrereleaseVersion = false
  }
}
