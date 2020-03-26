package com.e4d.pipeline

import com.e4d.shell.ShellImpl

class PipelineShell extends ShellImpl {
  def pipeline
  
  PipelineShell(pipeline) {
    this.pipeline = pipeline
  }
  
  def execute(Map args, List env) {
    args.script = sanitizedScript(args.script)
    pipeline.withEnv(escape(env)) {
      pipeline.sh(args)
    }
  }

  def readFile(Map args) {
    pipeline.readFile(args.file)
  }

  def writeFile(Map args) {
    pipeline.writeFile(args)
  }

  def sanitizedScript(script) {
    if (script.startsWith('#!')) {
      return script
    } else {
      return ['#!/usr/bin/env bash', script].join('\n')
    }
  }
}
