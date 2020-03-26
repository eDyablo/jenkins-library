package com.e4d.shell

import com.cloudbees.groovy.cps.NonCPS

abstract class ShellImpl implements Shell {
  def execute(String script) {
    execute([script: script], [])
  }
  
  def execute(String script, List env) {
    execute([script: script], env)
  }

  def execute(Map args) {
    execute(args, [])
  }

  def writeFile(String file, String text) {
    writeFile(file: file, text: text, encoding: 'utf-8')
  }

  void exit() {
  }

  @NonCPS
  def escape(List<String> values) {
    values.collect {
      it.replaceAll('\\\\', '\\\\\\\\')
    }
  }

  void withShell(Closure code) {
    try {
      code.call(this)
    }
    finally {
      this.exit()
    }
  }
}
