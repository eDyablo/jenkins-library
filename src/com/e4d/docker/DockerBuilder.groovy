package com.e4d.docker

import com.e4d.build.CommandLineBuilder
import com.e4d.pipeline.PipelineShell
import com.e4d.shell.Shell
import groovy.util.GroovyCollections

class DockerBuilder {
  final def pipeline
  final CommandLineBuilder commandLineBuilder
  final Shell shell

  final String getRepoTagFormat = "{{ if ne 0 (len .RepoTags) }}{{ index .RepoTags 0 }}{{ else }}<none>{{ end }}"
  final String outputMark = 'write-output'
  final registry = [:]

  DockerBuilder(pipeline, Shell shell) {
    this.pipeline = pipeline
    this.commandLineBuilder = new CommandLineBuilder(
      optionKeyValueSeparator: '=')
    this.shell = shell
  }

  DockerBuilder(pipeline) {
    this(pipeline, new PipelineShell(pipeline))
  }

  def useRegistry(String name, String user, String password) {
    registry.name = name
    registry.user = user
    registry.password = password
  }

  def build(Map options, String path) {
    final String dockerBuildFile =
      [path, 'Dockerfile.build'].join('/')
    def output = ''
    if (pipeline.fileExists(dockerBuildFile)) {
      output = multiPhaseBuild(options, path)
    } else {
      output = singlePhaseBuild(options, path)
    }
    final def records = output.split('\n')
      .findAll{ it.startsWith(outputMark) }
      .collect{ (it - outputMark).trim() }
    final def ids = records.take(1)?.find{ true }?.split() ?: []
    ids.inject([]) { accum, id ->
      accum += [id: id]; accum
    }
  }

  def singlePhaseBuild(Map options, String path) {
    final def buildOptions = options + [
      file: [path, 'Dockerfile'].join('/'),
      quiet: true
    ]
    final def commands = [
      '#!/usr/bin/env bash',
      'set -o errexit',
      registry ? commandLineBuilder.buildCommand(['docker', 'login'], [registry.name],
        username: registry.user, password: registry.password) : '',
      "id=\$(${ commandLineBuilder.buildCommand(['docker', 'build'], [path], buildOptions) })",
      "echo ${ outputMark } \$id",
    ]
    shell.execute([], script: commands.join('\n'),
      returnStdout: true, encoding: 'utf-8')
  }

  def multiPhaseBuild(Map options, String path) {
    final String containerBuildPath = '/build'
    final String buildPath = 'build'
    final def buildOptions = options + [
      file: [path, 'Dockerfile.build'].join('/'),
      build_arg : (options.build_arg ?: []) +
        ["build_path=${ containerBuildPath }"],
      quiet: true
    ]
    final def composeOptions = options + [
      file: [path, 'Dockerfile'].join('/'),
      build_arg : (options.build_arg ?: []) +
        ["build_path=${ buildPath }"],
      quiet: true
    ]
    final def commands = [
      '#!/usr/bin/env bash',
      'set -o errexit',
      registry ? commandLineBuilder.buildCommand(['docker', 'login'], [registry.name],
        username: registry.user, password: registry.password) : '',
      "ids=\$(${ commandLineBuilder.buildCommand(['docker', 'build'], [path], buildOptions) })",
      "container=\$(${ commandLineBuilder.buildCommand(['docker', 'create'], ['$ids']) })",
      commandLineBuilder.buildCommand(['docker', 'cp'],
        ["\$container:${ containerBuildPath }", [path, buildPath].join('/')]),
      commandLineBuilder.buildCommand(['docker', 'rm'], ['$container'], [force: true]),
      "ids=\"\$ids \$(${ commandLineBuilder.buildCommand(['docker', 'build'], [path], composeOptions) })\"",
      "echo ${ outputMark } \$ids",
    ]
    shell.execute([], script: commands.join('\n'),
      returnStdout: true, encoding: 'utf-8')
  }
}
