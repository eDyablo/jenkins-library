#!groovy

def execute(String script) {
  return sh(script: script,
    returnStdout: false)
}

def eval(String script) {
  return sh(script: """
    set +x
    ${script}
    set -x""",
    returnStdout: true,
    encoding: 'utf-8').trim()
}
