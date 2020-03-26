package com.e4d.job

class JobNotifyDecl extends JobDeclaration {
  def successPublishers
  def failurePublishers

  JobNotifyDecl(def script) {
    super(script)
  }

  def success(def closure) {
    declare(new JobResultDecl(script, successPublishers), closure)
  }

  def failure(def closure) {
    declare(new JobResultDecl(script, failurePublishers), closure)
  }
}
