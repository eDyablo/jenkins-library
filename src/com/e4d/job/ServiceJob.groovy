package com.e4d.job

class ServiceJob extends PipelineJob {
  Closure pipelineClosure

  ServiceJob(def pipeline) {
    super(pipeline)
  }

  def servicePipeline(Closure closure) {
    pipelineClosure = closure
  }

  def run() {
    pipelineClosure.delegate = pipeline
    pipelineClosure.resolveStrategy = Closure.DELEGATE_ONLY
    pipelineClosure()
  }
}
