package com.e4d.python

import com.e4d.shell.Shell

class PytestTool {
  final def pipeline
  final Shell shell

  PytestTool(pipeline, Shell shell) {
    this.pipeline = pipeline
    this.shell = shell
  }

  def test(Map options=[:]) {
    test(options, '.')
  }

  def test(Map options=[:], String baseDir) {
    final def includeNames = ([] + options.includeFileNames).findAll()
    final def excludeNames = ([] + options.excludeFileNames).findAll()
    final def excludePaths = ([] + options.excludePaths).findAll()
    final def includeFileExpr = includeNames
      .collect { "-name '${ it }'" }
      .join(' -or ')
    final def excludeFileExpr = excludeNames
      .collect { "-not -name '${ it }'" }
      .join(' -and ')
    final def excludePathExpr = excludePaths
      .collect { "-not -path '${ it }'" }
      .join(' -and ')
    final def findActionExpr = [includeFileExpr, excludeFileExpr, excludePathExpr]
      .findAll()
      .collect { "\\( ${ it } \\)" }
      .join(' -and ')
    final def script = """\
      #!/usr/bin/env bash
      set -o errexit
      set -o pipefail
      base_dir="${ baseDir }"
      find_expression="${ findActionExpr }"
      test_files="\$(eval "find "\${base_dir}" "\${find_expression}"")"
      if [[ ! -z "\${test_files}" ]]
      then
        pip install pytest
        set +o errexit
        python -m pytest \${test_files} --verbose; rc=\${?}; [ \${rc} == 5 ] && exit 0 || exit \${rc}
      fi
    """.stripIndent()
    shell.execute(script: script, [])
  }
}
