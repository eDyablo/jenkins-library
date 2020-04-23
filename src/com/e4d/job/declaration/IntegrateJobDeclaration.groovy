package com.e4d.job.declaration

import com.e4d.declaration.Declaration
import com.e4d.job.IntegrateJob

class IntegrateJobDeclaration extends Declaration {
  final IntegrateJob job

  IntegrateJobDeclaration(IntegrateJob job) {
    this.job = job
  }

  void artifact(Closure definition) {
    define(IntegrateArtifactDeclaration.newInstance(job), definition)
  }

  void source(Closure definition) {
    define(IntegrateSourceDeclaration.newInstance(job), definition)
  }

  void publish(Closure definition) {
    define(PublishDeclaration.newInstance(job), definition)
  }

  void publishStrategy(Closure definition) {
    define(PublishStrategyDeclaration.newInstance(job), definition)
  }

  def getArtifact() {
    IntegrateArtifactDeclaration.newInstance(job)
  }

  def getPublishStrategy() {
    PublishStrategyDeclaration.newInstance(job)
  }

  def getSource() {
    IntegrateSourceDeclaration.newInstance(job)
  }
}
