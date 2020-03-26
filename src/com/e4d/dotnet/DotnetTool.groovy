package com.e4d.dotnet

import com.cloudbees.groovy.cps.NonCPS
import com.e4d.build.CommandLineBuilder
import com.e4d.shell.Shell
import groovy.json.JsonSlurperClassic

/**
 * A tool for dealing with dotnet artefacts like C# projects, nuget packages etc.
 */
class DotnetTool {
  final Shell shell
  final String tool = 'dotnet'
  final def pipeline
  final CommandLineBuilder cmdBuilder

  static final String csproj = 'csproj'
  static final String nupkg = 'nupkg'
  static final def noEnv = []

  /**
   * Constructor
   *
   * @param pipeline  A reference to the pipeline job script uses the tool
   * @param shell     A reference to a shell that the tool uses to run its scripts
   */
  DotnetTool(pipeline, Shell shell) {
    this.pipeline = pipeline
    this.shell = shell
    cmdBuilder = new CommandLineBuilder()
  }

  /**
   * Run tests from all C# project that can be found under the specified directory.
   *
   * @param baseDir A path specifying the directory contains projects with tests
   * @param options A map of arbitrary options
   */
  def test(Map options=[:], String baseDir) {
    def resultsDir = options.resultsDir ?: '.'
    def includeProjects = options.includeProjects ?: ''
    def excludeProjects = options.excludeProjects ?: ''
    def testFilter = options.testFilter ?: ''
    def configuration = options.configuration ?: 'Debug'
    def env = [
      "BASE_DIR=\"${ options.baseDir ?: baseDir }\"",
      "INCLUDE_PROJECT_PATTERN=\"${ includeProjects }\"",
      "EXCLUDE_PROJECT_PATTERN=\"${ excludeProjects }\"",
      "TEST_FILTER=\"${ testFilter }\"",
      "RESULTS_DIR=\"${ resultsDir }\"",
      "BUILD_CONFIGURATION=\"${ configuration }\""
    ]
    shell.execute(script: pipeline.libraryResource('com/e4d/dotnet/dotnet-tool-test.sh'), 
      resultsDir: resultsDir, env)
  }

  /**
   * Pushes specified nuget package into repository.
   *
   * @param pkg             The name of the nuget package
   * @param options         A map of arbitraty options
   * @param otpions.source  An URL of server for the repository
   * @param otions.api_key  An API key to get access to the repository's API
   */
  def nugetPush(Map options=[:], String pkg) {
    final selector = getNugetSelector(pkg)
    final commands = [
      'set -o errexit',
      pkg.contains('/') ? "package='${ pkg }'" : "package=\$(find -name '${ selector }')",
      getNugetPushCommand(
        nuget: '${package}',
        source: options.source,
        api_key: options.api_key,
      )
    ]
    shell.execute(script: commands.join('\n'), [])
  }

  /**
   * Packs the specified C# project.
   *
   * @param proj                  The name of the project without csproj extension
   * @param options               A map of arbitraty options
   * @param options.version       A string denotes version of the package
   * @param options.versionPrefix A string represents version prefix consists
   *                              of major, minor and patch numbers separated
   *                              by dot
   * @param options.versionSuffix A string denotes version suffix
   *
   * @return  A map contains information about resulting package, contains
   *          project, package, version
   */
  def csprojPack(Map options=[:], String project) {
    final projectSelector = getProjectSelector(project)
    final packageSelector = "*.${ nupkg }"
    final commands = [
      'set -o errexit',
      "project=\$(find -name '${ projectSelector }')",
      'project_output=$(mktemp --directory --quiet)',
      getPackCommand(
        project: '${project}',
        output: '${project_output}',
        version: options.version,
        versionPrefix: options.versionPrefix,
        versionSuffix: options.versionSuffix,
      ),
      "package=\$(find \"\${project_output}\" -name '${ packageSelector }')",
      getEchoCommand(
        selector: projectSelector,
        project: '${project}',
        package: '${package}',
      ),
    ]
    final result = extractResult(shell.execute(
      script: commands.join('\n'),
      returnStdout: true,
      noEnv
    ))
    result + [
      version: extractVersion(result)
    ]
  }

  private String getProjectSelector(String name) {
    name.endsWith(".${ csproj }") ? name : [name, csproj].join('.')
  }

  private String getNugetSelector(String name) {
    name.endsWith(".${ nupkg }") ? name : [name, nupkg].join('.')
  }

  private String getPackCommand(Map options) {
    [
      'dotnet',
      'pack',
      cmdOpt('output', dqw(options.output)),
      cmdFlag('no-build'),
      cmdProp('Version', options.version),
      cmdProp('VersionPrefix', options.versionPrefix),
      cmdOpt('version-suffix', options.versionSuffix),
      dqw(options.project),
    ].findAll().join(' ')
  }

  private String getNugetPushCommand(Map options) {
    [
      'dotnet',
      'nuget',
      'push',
      cmdOpt('source', options.source),
      cmdOpt('api-key', options.api_key),
      options.nuget,
    ].join(' ')
  }

  private String dqw(String original) {
    ['"', original, '"'].join('')
  }

  private String cmdOpt(String name, String value) {
    value ? [cmdFlag(name), value].join('=') : ''
  }

  private String cmdFlag(String name) {
    name ? ['--', name].join('') : ''
  }

  private String cmdProp(String name, String value) {
    value ? [['-p', name].join(':'), value].join('=') : ''
  }

  private String getEchoCommand(Map values) {
    final lines = ['echo "{']
    lines.add(values.collect { key, value ->
      "  \\\"${ key }\\\": \\\"${ value }\\\"".toString()
    }.join(',\n'))
    lines.add('}"')
    lines.join('\n')
  }

  @NonCPS
  private Map extractResult(String text) {
    final lines = (text?.split('\n') ?: []).dropWhile { it != '{' }.takeWhile { it != '}' }
    if (lines) {
      new JsonSlurperClassic().parseText(lines.join('\n') + '\n}')
    } else {
      [:]
    }
  }

  private String extractVersion(Map options) {
    final project = (options.project?.split('/')?.last() ?: '') - ".${ csproj }"
    (options.package?.split('/')?.last() ?: '') - "${ project }." - ".${ nupkg }"
  }
}
