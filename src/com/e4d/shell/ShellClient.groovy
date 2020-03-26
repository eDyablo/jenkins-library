package com.e4d.shell

class ShellClient {
  def pipeline

  ShellClient(pipeline) {
    this.pipeline = pipeline
  }

  def execute(String command) {
    pipeline.sh(script: command, returnStdout: true, encoding: 'utf-8').trim()
  }

  def execute(Object[] commands) {
    execute(commands.collect{it.toString()}.join('\n'))
  }

  def hidden(String command) {
    "{\n$command\n} 2> /dev/null"
  }

  def hidden(String[] commands) {
    "{\n${commands.join('\n')}\n} 2> /dev/null"
  }

  def piped(String[] commands) {
    commands.join(' | ')
  }
}
