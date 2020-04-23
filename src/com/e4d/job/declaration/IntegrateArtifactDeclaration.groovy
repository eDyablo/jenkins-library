package com.e4d.job.declaration

import com.e4d.declaration.Declaration
import com.e4d.job.IntegrateJob

class IntegrateArtifactDeclaration extends Declaration {
  final IntegrateJob job

  IntegrateArtifactDeclaration(IntegrateJob job) {
    this.job = job
  }

  void baseName(String name) {
    job.artifactBaseName = name
  }
}
