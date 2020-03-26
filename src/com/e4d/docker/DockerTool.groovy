package com.e4d.docker

import com.e4d.shell.Shell
import java.security.MessageDigest

class DockerTool {
  final Shell shell

  DockerTool(Shell shell) {
    this.shell = shell
  }

  String build(Map options=[:]) {
    final path = options.path ?: '.'
    final imageIdFile = getImageIdFile(path)
    shell.execute(script: [
      'docker build',
      cmdOpt('build-arg', options.buildArgs),
      cmdOpt('network', options.network),
      cmdOpt('iidfile', imageIdFile),
      path,
    ].findAll().join(' '), [])
    shell.readFile(file: imageIdFile)
  }

  void login(Map options=[:]) {
    final envs = []
    final words = [
      'docker login',
      options.server,
    ]
    if (options.username) {
      envs.add(cmdEnv('login_username', options.username))
      words.add(cmdOpt('username', '${login_username}'))
    }
    if (options.password) {
      envs.add(cmdEnv('login_password', options.password))
      words.add(cmdOpt('password', '${login_password}'))
    }
    shell.execute(script: words.findAll().join(' '), envs)
  }

  String getImageIdFile(String key) {
    final digest = MessageDigest.getInstance('MD5').digest(key.bytes).encodeHex()
    final name = [this.class.simpleName, hashCode(), 'build', digest].join('.')
    ['/tmp', name].join('/')
  }

  private String cmdEnv(String name, String value) {
    [name, value].join('=')
  }

  private String cmdOpt(String option, Map<String, String> args) {
    args.collect { name, value ->
      cmdOpt(option, "'${ name }=${ value }'")
    }.join(' ')
  }

  private String cmdOpt(String name, String value) {
    "--${ name }=${ value }".toString()
  }
}
