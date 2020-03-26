package com.e4d.k8s

import com.cloudbees.groovy.cps.NonCPS
import com.e4d.build.CommandLineBuilder
import com.e4d.k8s.K8sResponse
import com.e4d.shell.ShellClient
import groovy.json.JsonSlurperClassic

class K8sClient extends ShellClient {
  def pipeline
  Map scriptsMap
  String configPath
  String context = 'default'

  final CommandLineBuilder commandLineBuilder = new CommandLineBuilder(
    optionKeyValueSeparator: '=')

  static final String ALL_NAMESPACES = '*all'

  K8sClient(pipeline) {
    super(pipeline)
    this.pipeline = pipeline
  }

  @NonCPS
  void setContext(String value) {
    this.context = value?.trim() ?: ''
  }

  @NonCPS
  void setConfigPath(String value) {
    this.configPath = value?.trim()
  }

  def useContext(String newContext) {
    final previousContext = context
    setContext(newContext)
    return previousContext
  }

  def getDeployment(String name, String namespace = 'default') {
    getResource('deployment', name, namespace)
  }

  def getDeployments(String namespace = 'default') {
    getResource('deployment', '', namespace)
  }

  def getResource(String kind, String name, String namespace = 'default') {
    final raw = getRawResource(kind, name, namespace)
    final parsed = new JsonSlurperClassic().parseText(raw)
    new K8sResponse(parsed)
  }

  def getRawResource(String kind, String name, String namespace = 'default') {
    final output = pipeline.sh(script: buildCommandBlock([
        getResourceCommand(kind, name, namespace)]),
      returnStdout: true,
      encoding: 'utf-8').trim()
    return output ?: '{}'
  }

  def getResourceCommand(String kind, String name, String namespace = 'default') {
    final namespaceOption = namespace == ALL_NAMESPACES ?
        '--all-namespaces' : "--namespace=${namespace}"
    buildCommand([
      'get', kind, name,
      namespaceOption,
      '--output=json',
      '--ignore-not-found=true'
      ])
  }

  def buildCommand(def words) {
    ([commandHeader] + words).join(' ')
  }

  def buildCommandBlock(List commands) {
    commands.join('\n')
  }

  def deleteOutdatedResources(String kind, String namePattern,
      int keepNumber = 10, String namespace = 'default') {
    final env = [
      "KIND=$kind",
      "NAMEPATTERN=$namePattern",
      "KEEPNUMBER=$keepNumber",
      "NAMESPACE=$namespace",
      "KUBECONFIG=${ configPath }",
      "CONTEXT=${ context ?: '' }",
    ]
    pipeline.withEnv(env) {
      pipeline.sh(scripts.deleteOutdatedResources)
    }
  }

  def renameResource(String kind, String oldName, String newName,
      String namespace = 'default') {
    final resource = getResource(kind, oldName, namespace)
    resource.data.metadata.name = newName
    execute(
      hidden(piped("echo '$resource'", "${ commandHeader } apply --filename -")),
      "${ commandHeader } delete $kind $oldName --namespace=$namespace --now"
    )
  }

  def deleteResource(Map kwargs) {
    final namespace = kwargs.namespace ?: 'default'
    final ignoreNotFound = kwargs.ignoreNotFound ?: false
    execute(
      (context),
      "${ commandHeader } delete $kwargs.kind $kwargs.name --namespace=$namespace --ignore-not-found=$ignoreNotFound"
    )
  }

  def deleteResource(Map kwargs, String kind, String name) {
    deleteResource(kwargs + [kind: kind, name: name])
  }

  def deleteResource(String kind, String name, String namespace = 'default') {
    deleteResource([kind: kind, name: name, namespace: namespace])
  }

  def applyResource(Map kwargs) {
    final namespace = kwargs.namespace ?: 'default'
    execute("${ commandHeader } apply --filename=$kwargs.path --namespace=$namespace")
  }

  def applyResource(Map kwargs, String path, String namespace = 'default') {
    applyResource(kwargs + [path: path, namespace: namespace])
  }

  def applyResource(String path, String namespace = 'default') {
    applyResource(path: path, namespace: namespace)
  }

  def apply(Map options = [:], List paths) {
    options = globalOptions + options
    final dataSelectors = [
      kind: '{{.kind}}',
      creationTimestamp: '{{.metadata.creationTimestamp}}',
      name: '{{.metadata.name}}',
      namespace: '{{.metadata.namespace}}',
      resourceVersion: '{{.metadata.resourceVersion}}',
      uid: '{{.metadata.uid}}',
    ]
    final String prefix = 'modified:'
    final String delimiter = '%'
    final String terminator = ';'
    final String recordTemplate = [
      prefix,
      dataSelectors.values().join(delimiter),
      terminator
    ].join('')
    options += [
      filename: paths,
      output: "go-template",
      template: """\
        {{- if .items -}}
          {{- range .items -}}
            ${ recordTemplate }
          {{- end -}}
        {{- else -}}
          ${ recordTemplate }
        {{- end -}}""".stripIndent()
    ]
    final commands = commandLineBuilder.buildCommand(
      ['kubectl', 'apply'], [], options)
    final output = pipeline.sh(script: commands, returnStdout: true,
      encoding: 'utf-8')
    final lines = output.tokenize("${ terminator }\n")
    lines.findAll {
      it.startsWith(prefix)
    }.collect { line ->
      final tokens = (line - prefix).tokenize(delimiter)
      final resource = [:]
      zip(dataSelectors.keySet(), tokens) { key, value ->
        resource[key] = value
      }
      resource
    }
  }

  def createNamespace(Map options=[:], List<String> names) {
    final script = names.inject(new StringBuilder('set -o errexit')) {
      commands, name ->
      commands << '\n' << createNamespaceCommand(options, name)
    }.toString()
    pipeline.sh(script)
  }

  def createNamespace(Map options=[:], String[] names) {
    createNamespace(options, names as List<String>)
  }

  String createNamespaceCommand(Map options=[:], String name) {
    options = globalOptions + options
    options += [dry_run: true, output: 'yaml', save_config: true]
    final create = commandLineBuilder.buildCommand(
      ['kubectl', 'create', 'namespace', name], [], options)
    final apply = commandLineBuilder.buildCommand(
      ['kubectl', 'apply'], [], globalOptions + [filename: '-'])
    [create, apply].join(' | ')
  }

  static void zip(first, second, Closure consumer) {
    final firstIter = first.iterator()
    final secondIter = second.iterator()
    while (firstIter.hasNext() && secondIter.hasNext()) {
      consumer(firstIter.next(), secondIter.next())
    }
  }

  def waitDeployment(Map options = [:], String deployment) {
    final env = [
      "DEPLOYMENT=${ deployment }",
      "NAMESPACE=${ options.namespace ?: '' }",
      "CHECK_INTERVAL=${ options.checkInterval ?: '' }",
      "COMPLETION_TIMEOUT=${ options.completionTimeout ?: '' }",
      "KUBECONFIG=${ configPath }",
      "CONTEXT=${ context ?: '' }",
    ]
    pipeline.withEnv(env) {
      pipeline.sh(scripts.waitDeployment)
    }
  }

  def getServer(String context) {
    final env = [
      "KUBECONFIG=${ configPath }",
      "CONTEXT=${ context ?: '' }",
    ]
    pipeline.withEnv(env) {
      final data = pipeline.sh(
        script: scripts.getServer,
        returnStdout: true,
        encoding: 'utf-8').trim().tokenize(' ')
      [url: data[0], creds: data[1]]
    }
  }

  def getResourceData(Map options = [:], String kind, List names = []) {
    options = globalOptions + options
    final commands = [
      '#!/usr/bin/env bash',
      commandLineBuilder.buildCommand(['kubectl', 'get'], [kind] + names, options),
    ]
    pipeline.sh(script: commands.join('\n'),
      returnStdout: true, encoding: 'utf-8')
  }

  def getResources(Map options = [:], List resources) {
    options = globalOptions + options
    final script = resources.inject([]) { commands, resource ->
      final resourceOptions = options +
        [namespace: resource.namespace ?: options.namespace]
      commands += commandLineBuilder.buildCommand(
        ['kubeclt', 'get'], [resource.kind, resource.name], resourceOptions)
      commands
    }
    pipeline.sh(script: script.join('\necho "---"\n'),
      returnStdout: true, encoding: 'utf-8')
  }

  def getAppliedConfig(Map options = [:], String kind, List names = []) {
    options = globalOptions + options
    final commands = [
      '#!/usr/bin/env bash',
      commandLineBuilder.buildCommand(
        ['kubectl', 'apply', 'view-last-applied'], [kind] + names, options),
    ]
    pipeline.sh(script: commands.join('\n'),
      returnStdout: true, encoding: 'utf-8')
  }

  def getAppliedConfig(Map options = [:], List resources) {
    options = globalOptions + options
    final script = resources.inject([]) { commands, resource ->
      final resourceOptions = options +
        [namespace: resource.namespace ?: options.namespace]
      final checker = commandLineBuilder.buildCommand(
        ['kubectl', 'get'], [resource.kind, resource.name],
        globalOptions + [namespace: resource.namespace ?: options.namespace])
      final getter = commandLineBuilder.buildCommand(
        ['kubectl', 'apply', 'view-last-applied'],
        [resource.kind, resource.name], resourceOptions)
      commands += "if ${ checker } > /dev/null; then ${ getter }; echo '---'; fi"
      commands
    }
    pipeline.sh(script: script.join('\n'),
      returnStdout: true, encoding: 'utf-8')
  }

  def delete(Map options = [:], List resources) {
    options = globalOptions + options
    final script = resources.inject([]) { commands, resource ->
      final resourceOptions = options +
        [namespace: resource.namespace ?: options.namespace]
      commands += commandLineBuilder.buildCommand(['kubectl', 'delete'],
        [resource.kind, resource.name], resourceOptions)
      commands
    }
    pipeline.sh(script: script.join('\n'))
  }

  def getServicePodLabel(Map options = [:], String service, String label) {
    final namespace = options.namespace ?: 'default'
    pipeline.sh(script: 
    "${ commandHeader } --namespace=${ namespace } get pod --selector=app=\$(${ commandHeader } --namespace=${ namespace } get svc ${ service } --output='go-template={{ index .spec.selector \"app\" }}') " +
    "--output='go-template={{ index (index .items 0).metadata.labels \"${ label }\" }}'"
    , returnStdout: true)
  }

  void createDockerRegistrySecret(Map options) {
    final script = ('#!/usr/bin/env bash\n') << commandHeader
    if (options.namespace?.trim()) {
      script << " --namespace=${ options.namespace?.trim() }"
    }
    script << ' create secret' <<
    " docker-registry ${ validSecretName(options.name) }" <<
    " --docker-server=${ validDockerRegistryServer(options.server) }" <<
    " --docker-username=${ validDockerRegistryUser(options.user) }" <<
    " --docker-password=${ validDockerRegistryPassword(options.password) }" <<
    ' || exit 0'
    pipeline.sh(script: script.toString())
  }

  String validSecretName(String name) {
    validTextArgument(name, "Name of the secret")
  }

  String validDockerRegistryServer(String name) {
    validTextArgument(name, "Docker registry server")
  }

  String validDockerRegistryUser(String name) {
    validTextArgument(name, "Docker registry user")
  }

  String validDockerRegistryPassword(String name) {
    validTextArgument(name, "Docker registry password")
  }

  String validTextArgument(String argument, String messagePrefix) {
    if (argument == null) {
      throw new IllegalArgumentException("${ messagePrefix } is null")
    }
    if (argument == '') {
      throw new IllegalArgumentException("${ messagePrefix } is empty")
    }
    if (argument.trim() == '') {
      throw new IllegalArgumentException("${ messagePrefix } is whitespace")
    }
    return argument
  }

  String getCommandHeader() {
    final text = new StringBuilder('kubectl')
    globalOptions.each { key, value ->
      text << ' --' << key << '=\'' << value << '\''
    }
    text.toString()
  }

  Map getGlobalOptions() {
    final options = [:]
    if (configPath) {
      options.kubeconfig=configPath
    }
    if (context) {
      options.context=context
    }
    return options
  }

  Map getScripts() {
    if (scriptsMap == null) {
      scriptsMap = [
        initialize: pipeline.libraryResource('com/e4d/k8s/k8s-client-init.sh'),
        waitDeployment: pipeline.libraryResource(
          'com/e4d/k8s/k8s-client-wait-deployment.sh'),
        deleteOutdatedResources: pipeline.libraryResource(
          'com/e4d/k8s/k8s-client-delete-outdated-resources.sh'),
        getServer: pipeline.libraryResource(
          'com/e4d/k8s/k8s-client-get-server.sh'),
      ]
    }
    return scriptsMap
  }
}
