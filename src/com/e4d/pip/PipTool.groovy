package com.e4d.pip

import com.e4d.build.CommandLineBuilder

class PipTool {
  def pipeline

  final String tool
  final CommandLineBuilder commandLineBuilder = new CommandLineBuilder()

  PipTool(pipeline) {
    this.pipeline = pipeline
  }

  def install(Map options=[:], String pkg) {
    def command = commandLineBuilder.buildCommand([tool, 'install'], [pkg], options)
    pipeline.sh(command)
  }
}
