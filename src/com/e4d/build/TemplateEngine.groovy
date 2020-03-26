package com.e4d.build

import com.cloudbees.groovy.cps.NonCPS

class TemplateEngine {
  final def pipeline

  TemplateEngine(pipeline) {
    this.pipeline = pipeline
  }

  def render(String input, Map values = [:]) {
    final def template = new groovy.text.StreamingTemplateEngine()
      .createTemplate(input)
    final def bindings = makeBindings(values)
    template.make(bindings).toString()
  }

  @NonCPS
  def makeBindings(Map source) {
    def bindings = [:]
    pipeline.env.getEnvironment().each { key, value ->
      bindings[key] = value
    }
    source.each { key, value ->
      bindings[key] = value
    }
    bindings["te"] = this
    bindings["env"] = pipeline.env
    bindings.withDefault{ null }
  }

  def render(List input, Map values = [:]) {
    input.inject([]) { output, item ->
      output += render(item, values)
      output
    }
  }

  def render(Map input, Map values = [:]) {
    input.inject([:]) { output, key, value ->
      output += ["$key": render(value, values)]
      output
    }
  }

  def render(Object[] input, Map values = [:]) {
    render(input as List, values)
  }

  def render(Object input, Map values = [:]) {
    render(input.toString, values)
  }

  @NonCPS
  def jobArtifactName(String suffix = '') {
    def jobName = pipeline.env.JOB_NAME
    def jobBaseName = pipeline.env.JOB_BASE_NAME.replace(".", "-")
    def buildId = pipeline.env.BUILD_ID
    "AH${NameUtils.shortMinutelyUniqueName(jobName)}-$jobBaseName-$buildId$suffix"
        .toLowerCase()
  }
}
