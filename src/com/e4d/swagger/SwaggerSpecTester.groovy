package com.e4d.swagger

import com.e4d.build.CommandLineBuilder

class SwaggerSpecTester {
  final CommandLineBuilder cmdBuilder
  final def diffOptions = [
    ignore_space_change: true,
    ignore_tab_expansion: true,
    ignore_trailing_space: true,
    new_file: true,
  ]
  final def pipeline

  SwaggerSpecTester(pipeline) {
    this.cmdBuilder = new CommandLineBuilder(optionKeyValueSeparator: '=')
    this.pipeline = pipeline
  }

  def test(workspacePath, image, imagePath) {
    workspacePath = pipeline.sh(script:"find ${ workspacePath }/src -type f -name swagger.json", returnStdout: true)
    workspacePath = workspacePath.trim()
    def referencePath = UUID.randomUUID().toString()
    def createContainer = cmdBuilder.buildCommand(['docker', 'create'], [image], rm: true)
    def copySpec = cmdBuilder.buildCommand(['docker', 'cp'], [ "\$container:${ imagePath }", referencePath])
    def check = cmdBuilder.buildCommand(['diff'], [workspacePath, referencePath],
      diffOptions + [brief: true])
    def diff = cmdBuilder.buildCommand(['diff'], [workspacePath, referencePath],
      diffOptions + [suppress_common_line: true])
    def removeContainer = cmdBuilder.buildCommand(['docker', 'rm'], ['$container'])
    def script = """\
      #!/usr/bin/env bash
      set -o errexit
      container=\$(${ createContainer })
      ${ copySpec } || true
      ${ removeContainer }
      if [ -f ${ workspacePath } ]; then
        if [ -f ${ referencePath } ]; then
          if [ ! -z "\$(${ check })" ]; then
            ${ diff }
            echo "Swagger specification doesn't much the service's API." >&2; exit 1
          fi
        fi
      fi
    """
    pipeline.sh(script.stripIndent())
  }
}
