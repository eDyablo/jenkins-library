package com.e4d.helm

import com.cloudbees.groovy.cps.NonCPS
import com.e4d.shell.Shell

class HelmTool {
  final Shell shell

  HelmTool(Shell shell) {
    this.shell = shell
  }

  def packageChart(Map options) {
    final args = []
    if (options?.version?.trim()) {
      args << '--version' << options.version
    }
    if (options?.updateDependencies) {
      args << '--dependency-update'
    }
    final chartPath = (options?.chartPath?.trim()) ? qw(options.chartPath) : '.'
    args << chartPath
    final output = shell.execute([],
      script: """\
        helm package ${ args.join(' ') } &&
        helm inspect chart ${ chartPath }
      """.stripIndent(),
      returnStdout: true,
    )
    return [
      name: extractChartName(output),
      path: extractPackagePath(output),
    ]
  }

  @NonCPS
  def extractPackagePath(String output) {
    final lines = output?.split('\n')
    final success = lines.find { it.startsWith('Successfully packaged chart and saved it to:') }
    if (success) {
      success.dropWhile { it != ':' }.dropWhile { it == ':' || it == ' '}
    }
  }

  @NonCPS
  def extractChartName(String output) {
    final lines = output?.split('\n')
    final success = lines.find { it.startsWith('name:') }
    if (success) {
      success.dropWhile { it != ':' }.dropWhile { it == ':' || it == ' '}
    }
  }

  def lintChart(Map options) {
    final args = []
    if (options?.path?.trim()) {
      args << qw(options.path)
    } else {
      args << '.'
    }
    final output = shell.execute([],
      script: "helm lint ${ args.join(' ') }".toString(),
      returnStdout: true,
    )
    final lines = output?.split('\n') as List<String>
    return [
      messages: extractInfoMessages(lines),
      warnings: extractWarnings(lines),
      errors: extractErrors(lines),
    ]
  }

  def fetchChart(Map options) {
    final envs = []
    final inspectArgs = []
    final fetchArgs = []
    if (options.user?.trim()) {
      envs << "user=${ options.user }".toString()
      inspectArgs << '--username' << '"${user}"'
      fetchArgs << '--username' << '"${user}"'
    }
    if (options.password?.trim()) {
      envs << "password=${ options.password }".toString()
      inspectArgs << '--password' << '"${password}"'
      fetchArgs << '--password' << '"${password}"'
    }
    if (options.unpack) {
      fetchArgs << '--untar'
    }
    inspectArgs << qw(options.chartURL)
    fetchArgs << qw(options.chartURL)
    final output = shell.execute(envs, returnStdout: true, script: """\
      helm inspect chart ${ inspectArgs.join(' ') } &&
      helm fetch ${ fetchArgs.join(' ') }
    """.stripIndent())
    final chart = extractChartInfo(output)
    final chartPath = options.chartURL?.split('/')?.last()
    if (chartPath) {
      chart.path = chartPath
    }
    if (options.unpack) {
      chart.path = chart.name
    }
    return chart
  }

  def upgradeChartRelease(Map options=[:], String release, String chart) {
    final args = []
    args << release << qw(chart)
    if (options.install) {
      args << '--install'
    }
    if (options.kubeConfig) {
      args << '--kubeconfig' << qw(options.kubeConfig)
    }
    if (options.kubeContext) {
      args << '--kube-context' << qw(options.kubeContext)
    }
    if (options.namespace) {
      args << '--namespace' << options.namespace
    }
    options.valueFiles.findAll().each {
      args << '--values' << qw(it)
    }
    if (options.wait) {
      args << '--wait'
    }
    shell.execute([],
      script: "helm upgrade ${ args.join(' ') }".toString())
  }

  def extractInfoMessages(List<String> lines) {
    extractMessages(lines, '[INFO]')
  }

  def extractWarnings(List<String> lines) {
    extractMessages(lines, '[WARNING]')
  }

  def extractErrors(List<String> lines) {
    extractMessages(lines, '[ERROR]')
  }

  def extractMessages(List<String> lines, String kind) {
    final prefixSize = kind.size()
    lines.findAll {
      it.startsWith(kind)
    }.collect {
      it.substring(prefixSize).trim()
    }
  }

  @NonCPS
  def extractChartInfo(String text) {
    final traits = ['name', 'description', 'version']
    final lines = text?.split('\n') ?: []
    lines.inject([:]) { info, line ->
      traits.collect {
        [it, it + ':']
      }.each { name, template ->
        if (line.startsWith(template)) {
          info[name] = line.substring(template.size()).trim()
        }
      }
      info
    }
  }

  String qw(String text) {
    "'${ text }'"
  }
}
