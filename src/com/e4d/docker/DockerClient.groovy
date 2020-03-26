package com.e4d.docker

import com.e4d.build.Path
import com.e4d.build.CommandLineBuilder

class DockerClient {
  final def script
  final CommandLineBuilder cmdBuilder

  DockerClient(def script) {
    this.script = script
    this.cmdBuilder = new CommandLineBuilder(
      optionKeyValueSeparator: '=')
  }

  // @Deprecated
  // Use build instead
  def buildImage(String path, String tag, Map args = [:]) {
    def buildArgs = args.collect { "--build-arg $it.key=\"$it.value\"" }.join(' ')
    script.sh("docker build '${Path.directory(path)}' --network=host --file='${path}' --tag=${tag} $buildArgs")
  }

  def build(Map options = [:], String path) {
    script.sh(cmdBuilder.buildCommand(
      ['docker', 'build'], [path], options))
  }

  def pushImage(String name, String registry, String user, String password) {
    def commands = [
      '#!/usr/bin/env bash',
      'set -o errexit',
      'set +x',
      cmdBuilder.buildCommand(['docker', 'login'], [registry],
        username: user, password: password),
      'set -x',
      cmdBuilder.buildCommand(['docker', 'push'], [name]),
    ]
    script.sh(commands.join('\n'))
  }

  def pushImage(String id, String name, String registry, String user, String password) {
    def commands = [
      '#!/usr/bin/env bash',
      'set -o errexit',
      'set +x',
      cmdBuilder.buildCommand(['docker', 'login'], [registry],
        username: user, password: password),
      'set -x',
      cmdBuilder.buildCommand(['docker', 'tag'], [id, name]),
      cmdBuilder.buildCommand(['docker', 'push'], [name]),
    ]
    script.sh(commands.join('\n'))
  }

  def listImages() {
    script.sh('docker image ls')
  }

  def removeStoppedContainers(Map kwargs = [:]) {
    def forceOption = (kwargs.force ?: false) ? '-f' : ''
    script.sh("docker container prune $forceOption")
  }

  def removeUnusedImages(Map kwargs = [:]) {
    def forceOption = (kwargs.force ?: false) ? '-f' : ''
    script.sh("docker image prune $forceOption")
  }

  def removeImage(Map kwargs, String image = null) {
    image = image ?: kwargs.image
    def forceOption = (kwargs.force ?: false) ? '-f' : ''
    script.sh("docker image rm $forceOption $image")
  }

  def removeImage(String image) {
    removeImage(image: image)
  }

  String runCommand(Map options, String image) {
    script.sh(
      script: cmdBuilder.buildCommand(
        ['docker', 'run'], [image], options),
      returnStdout: true,
      encoding: 'utf-8'
    ).trim()
  }

  List<String> getAllContainers() {
    script.sh(
      script: cmdBuilder.buildCommand(
        ['docker', 'ps'], [], [all: true, quiet: true]),
      returnStdout: true,
      encoding: 'utf-8'
    ).split('\n') as List<String>
  }

  void stopContainers(Map options = [:], List containers) {
    script.sh(
      cmdBuilder.buildCommand(
        ['docker', 'stop'], containers, options))
  }

  void stopContainer(Map options = [:], String containerId) {
    stopContainers(options, [containerId])
  }

  List<String> getLogs(Map options = [:], String containerId) {
    script.sh(
      script: cmdBuilder.buildCommand(
        ['docker', 'logs'], [containerId], options),
      returnStdout: true,
      encoding: 'utf-8').split('\n') as List<String>
  }

  String inspectContainer(Map options = [:], String containerId) {
    script.sh(
      script: cmdBuilder.buildCommand(
          ['docker', 'container', 'inspect'], [containerId], options),
        returnStdout: true,
        encoding: 'utf-8').trim()
  }

  String getContainerIPAddress(String containerId) {
    inspectContainer(containerId, format: '{{ .NetworkSettings.IPAddress }}')
  }
}
