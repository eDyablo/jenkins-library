package com.e4d.tar

import com.e4d.build.CommandLineBuilder

class TarTool {
  final CommandLineBuilder commandLineBuilder
  final String tar = 'tar'
  final pipeline

  TarTool(pipeline) {
    this.pipeline = pipeline
    commandLineBuilder = new CommandLineBuilder(
      optionKeyValueSeparator: '=')
  }

  def packDirectory(String directory, String archive) {
    final command = commandLineBuilder.buildCommand(
      [tar], ['.'],
      create: true,
      directory: directory,
      file: archive,
      gzip: true)
    pipeline.sh(command)
  }

  def pack(Map options=[:], String archive, String[] files) {
    final directory = files.take(1).find{ true } ?: ''
    files = files.drop(1)
    if (options.ignoreNonExisting)
      files = getExistingFiles(directory, files as List)
    final command = commandLineBuilder.buildCommand(
      [tar], files as List,
      create: true,
      directory: directory,
      file: archive,
      gzip: true)
    pipeline.sh(command)
  }

  def unpack(String archive) {
    final command = commandLineBuilder.buildCommand(
      [tar], [],
      file: archive,
      extract: true,
      gzip: true)
    pipeline.sh(command)
  }

  def getExistingFiles(String directory, List files) {
    files.findAll {
      pipeline.fileExists("${ directory }/${ it }")
    }
  }
}
