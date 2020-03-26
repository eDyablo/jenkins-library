package com.e4d.job

import com.cloudbees.groovy.cps.NonCPS
import com.e4d.build.NameUtils
import org.jenkinsci.plugins.pipeline.modeldefinition.*

class PipelineJob extends JobDeclaration {
  def pipeline
  def successPublishers = []
  def failurePublishers = []
  String jobUID

  PipelineJob(def pipeline) {
    super(pipeline)
    if (pipeline == null)
      throw new Exception('No pipeline instance specified')
    this.pipeline = pipeline
    jobUID = NameUtils.shortTimelyUniqueName(
      [pipeline.env?.JOB_NAME, pipeline.env?.BUILD_ID].join(''))
  }

  def defineParameters() {
    []
  }

  def loadParameters(params) {
  }

  def defineEnvVars() {
    []
  }

  def defineVolumes() {
    []
  }

  def declare(Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.delegate = this
    closure.call()
  }

  void initializeJob() {
  }

  def notify(Closure closure) {
    def declaration = new JobNotifyDecl(pipeline)
    declaration.successPublishers = successPublishers
    declaration.failurePublishers = failurePublishers
    declare(declaration, closure)
  }

  def propagateSuccess() {
    def notification = basicNotification() + [status: 'success']
    successPublishers.each {
      sendNotification(it, notification)
    }
  }

  def propagateFailure() {
    def notification = basicNotification() + [status: 'failure']
    failurePublishers.each {
      sendNotification(it, notification)
    }
  }

  def basicNotification() {
    [
      job: [
        name: pipeline.JOB_NAME,
        baseName: pipeline.JOB_BASE_NAME
      ],
      build: [
        id: pipeline.BUILD_ID,
        number: pipeline.BUILD_NUMBER,
        url: pipeline.BUILD_URL
      ]
    ]
  }

  def sendNotification(publisher, notification) {
    try {
      publisher.publish(notification)
    }
    catch (any) {
      pipeline.echo("WARNING: Failed to send notification\n${ any }")
    }
  }

  def stage(String name, Closure code) {
    stage(name, true, code)
  }

  def stage(String name, condition, Closure code) {
    pipeline.stage(name) {
      if (condition as boolean) {
        return code()
      }
      else {
        markStageSkipped(name)
        return [:]
      }
    }
  }

  void markStageSkipped(String name) {
    Utils.markStageSkippedForConditional(name)
  }

  @NonCPS
  def buildReport(Map data) {
    def width = data.keySet()*.length().max() + 1
    data.collect { key, value ->
      "${ key.padRight(width) }${ value }"
    }.join('\n')
  }

  void printReport(Map data) {
    pipeline.echo(buildReport(data))
  }

  void printReport(String report) {
    pipeline.echo(report)
  }

  def getUsernamePassword(String credsId) {
    final creds = [pipeline.usernamePassword(
      credentialsId: credsId,
      usernameVariable: 'user',
      passwordVariable: 'password')]
    pipeline.withCredentials(creds) {
      return [pipeline.env.user, pipeline.env.password]
    }
  }
}
