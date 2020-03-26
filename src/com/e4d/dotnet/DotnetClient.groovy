package com.e4d.dotnet

import com.e4d.shell.ShellClient

class DotnetClient extends ShellClient {
  DotnetConfig config

  DotnetClient(pipeline, DotnetConfig config) {
    super(pipeline)
    this.config = config
  }

  def getVersion() {
    execute('dotnet --version')
  }

  def getInfo() {
    execute('dotnet --info')
  }

  def encodeForShell(String source) {
    source.replaceAll(/\|/, /\\|/)
        .replaceAll(/\(/, /\\(/)
        .replaceAll(/\)/, /\\)/)
  }

  def build(String baseDir, String include) {
    build(baseDir: baseDir, include: include, exclude: '')
  }

  def build(Map kwargs, String baseDir) {
    build(kwargs + [baseDir: baseDir])
  }

  def build(Map kwargs, String baseDir, String include) {
    build(kwargs + [baseDir: baseDir, include: include])
  }
  
  def build(Map kwargs) {
    def include = kwargs.include ?: ''
    def exclude = kwargs.exclude ?: ''
    def configuration = kwargs.configuration ?: 'Debug'
    pipeline.withEnv(["BASE_DIR=$kwargs.baseDir",
        "INCLUDE_PATTERN=${encodeForShell(include)}",
        "EXCLUDE_PATTERN=${encodeForShell(exclude)}",
        "BUILD_CONFIGURATION=$configuration"]) {
      execute(pipeline.libraryResource(
          'com/e4d/dotnet/dotnet-client-build.sh'))
    }
  }

  def test(String baseDir, String includeProjects) {
    test(baseDir: baseDir, includeProjects: includeProjects,
        excludeProjects: '')
  }

  def test(Map kwargs, String baseDir) {
    test(kwargs + [baseDir: baseDir])
  }

  def test(Map kwargs, String baseDir, String includeProjects) {
    test(kwargs + [baseDir: baseDir, includeProjects: includeProjects])
  }

  def test(Map kwargs) {
    def testResultsDirName = 'dotnet-test-results'
    def includeProjects = kwargs.includeProjects ?: ''
    def excludeProjects = kwargs.excludeProjects ?: ''
    def testFilter = kwargs.testFilter ?: ''
    def configuration = kwargs.configuration ?: 'Debug'
    pipeline.withEnv(["BASE_DIR=$kwargs.baseDir",
        "INCLUDE_PROJECT_PATTERN=${encodeForShell(includeProjects)}",
        "EXCLUDE_PROJECT_PATTERN=${encodeForShell(excludeProjects)}",
        "TEST_FILTER=${encodeForShell(testFilter)}",
        "TEST_RESULTS_DIR_NAME=$testResultsDirName",
        "BUILD_CONFIGURATION=$configuration"]) {
      pipeline.sh(pipeline.libraryResource(
          'com/e4d/dotnet/dotnet-client-test.sh'))
    }
    [dir: "$kwargs.baseDir/$testResultsDirName", filePattern: '**/test_result*.trx']
  }

  def runTool(Map kwargs) {
    def includeProjects = kwargs.includeProjects ?: ''
    def excludeProjects = kwargs.excludeProjects ?: ''
    def arguments = kwargs.arguments ?: []
    def options = kwargs.options.collect {
        it.startsWith('-') ? it : "--$it"
    }    
    pipeline.withEnv(["TOOL=$kwargs.tool",
        "COMMAND=$kwargs.command",
        "COMMAND_ARGUMENTS=${arguments.join(' ')}",
        "COMMAND_OPTIONS=${options.join(' ')}",
        "BASE_DIR=$kwargs.baseDir",
        "INCLUDE_PATTERN=${encodeForShell(includeProjects)}",
        "EXCLUDE_PATTERN=${encodeForShell(excludeProjects)}"]) {
      execute(pipeline.libraryResource(
          'com/e4d/dotnet/dotnet-client-run-tool.sh'))
    }
  }
}
