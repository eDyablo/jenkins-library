package com.e4d.job.declaration

import com.e4d.declaration.Declaration
import com.e4d.job.IntegrateJob

class PublishDeclaration extends Declaration {
  final IntegrateJob job

  PublishDeclaration(IntegrateJob job) {
    this.job = job
  }

  def getStrategy() {
    PublishStrategyDeclaration.newInstance(job)
  }

  void strategy(Closure definition) {
    define(PublishStrategyDeclaration.newInstance(job), definition)
  }
}
