package com.e4d.job.declaration

import com.e4d.declaration.Declaration
import com.e4d.git.GitSourceReference
import com.e4d.job.IntegrateJob

class IntegrateSourceDeclaration extends Declaration  {
  final IntegrateJob job

  IntegrateSourceDeclaration(IntegrateJob job) {
    this.job = job
  }

  void git(String reference) {
    job.gitSourceRef = new GitSourceReference(reference)
  }

  void git(Map options) {
    job.gitSourceRef = new GitSourceReference(options)
  }

  void git(Closure definition) {
    definition.delegate = [:]
    definition.call()
    job.gitSourceRef = new GitSourceReference(definition.delegate)
  }
}
