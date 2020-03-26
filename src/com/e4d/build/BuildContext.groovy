package com.e4d.build

class BuildContext {
  def script

  BuildContext(def script) {
    this.script = script
  }

  String jobName() {
    return script.JOB_BASE_NAME
  }

  String buildId() {
    return script.BUILD_ID
  }

  String slaveLabel() {
    String name = jobName().replaceAll(' ', '_')
    return "slv-${name}-${buildId()}".toLowerCase()
  }
}
