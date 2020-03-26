package com.e4d.python

import com.e4d.shell.Shell

class PipReqValidatorTool {
  final def pipeline
  final Shell shell

  PipReqValidatorTool (pipeline, Shell shell) {
    this.pipeline = pipeline
    this.shell = shell
  }

  def validateReqVersion(String baseDir='.') {
    pipeline.dir(baseDir) {
      def reqVersionChecker = pipeline.libraryResource(
        'com/e4d/python/requirements_version_enforce.py')

      shell.execute(script: reqVersionChecker, [])
    }
  }
}
