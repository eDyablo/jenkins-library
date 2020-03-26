package com.e4d.docker

import com.e4d.build.CommandLineBuilder
import com.e4d.shell.Shell
import com.e4d.shell.ShellImpl

class DockerImageShell extends ShellImpl {
  final CommandLineBuilder cmdBuilder
  final Shell shell
  final String image
  final Map options

  DockerImageShell(Map options=[:], Shell shell, image) {
    this.cmdBuilder = new CommandLineBuilder()
    this.shell = shell
    this.image = image
    this.options = options
  }

  def execute(Map args, List env) {
    final List shellEnv = options.env ?: []
    final String hostDir = 'scripts'
    final String guestDir = '/opt/scripts'
    final String script = UUID.randomUUID().toString()
    shell.writeFile(file: "${ hostDir }/${ script }", text: args.script)
    def createContainer = cmdBuilder.buildCommand(
      ['docker', 'create'], [image, '/bin/bash', "${ guestDir }/${ script }"],
      env: escape(shellEnv + env), network: options.network)
    def getWorkingDir = cmdBuilder.buildCommand(
      ['docker', 'inspect'], ['$id'], format: '{{ .Config.WorkingDir }}')
    def copyScript = cmdBuilder.buildCommand(
      ['docker', 'cp'], ["\$(pwd)/${ hostDir }", "\$id:${ guestDir }"])
    def startContainer = cmdBuilder.buildCommand(
      ['docker', 'start'], ['$id'], attach: true)
    def fetchResults = cmdBuilder.buildCommand(
      ['docker', 'cp'], [ "\$id:\$wd/${ args.resultsDir }", "${ args.resultsDir }/" ])
    def removeContainer = cmdBuilder.buildCommand(
      ['docker', 'rm'], ['$id']
    )
    def login = [
      'set +x',
      cmdBuilder.buildCommand(['docker', 'login'], [options.registry],
        username: options.user, password: options.password),
      'set -x'
    ].join('\n')
    def commands = [
      '#!/usr/bin/env bash',
      'set -o errexit',
      options.registry ? login : '',
      "id=\$(${ createContainer })",
      "wd=\$(${ getWorkingDir })",
      copyScript,
      "trap \'${ args.resultsDir ? fetchResults : 'echo "No ResultDir specified"' } && ${ removeContainer }\' EXIT",
      startContainer
    ]

    args.script = commands.join('\n')
    shell.execute(args, [])
  }

  def readFile(Map args) {
    throw new UnsupportedOperationException()
  }

  def writeFile(Map args) {
    throw new UnsupportedOperationException()
  }
}
