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
    final envs = []
    final commands = [
      'set -o errexit',
    ]
    if (options.registry) {
      final loginWords = [
        'docker', 'login',
      ]
      if (options.username) {
        envs.add(cmdEnv('login_username', options.username))
        loginWords.add(cmdOpt('username', '${login_username}'))
      }
      if (options.password) {
        envs.add(cmdEnv('login_password', options.password))
        loginWords.add(cmdOpt('password', '${login_password}'))
      }
      loginWords.add(options.registry)
      commands.add(loginWords.join(' '))
    }
    commands.add([
      'docker build',
      cmdOpt('build-arg', options.buildArgs),
      cmdOpt('network', options.network),
      cmdOpt('iidfile', imageIdFile),
      path,
    ].findAll().join(' '))
    shell.execute(script: commands.findAll().join('\n'), envs)
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

  void push(Map options=[:], String image) {
    final targetImage = [options.registry, options.name ?: image].findAll().join('/')
    final commands = [
      'set -o errexit',
    ]
    final envs = []
    if (options.registry) {
      final loginCmd = [
        'docker', 'login',
      ]
      if (options.username) {
        envs.add(cmdEnv('login_username', options.username))
        loginCmd.add(cmdOpt('username', '${login_username}'))
      }
      if (options.password) {
        envs.add(cmdEnv('login_password', options.password))
        loginCmd.add(cmdOpt('password', '${login_password}'))
      }
      loginCmd.add(options.registry)
      commands.add(loginCmd.join(' '))
    }
    if (options.registry || options.name) {
      commands.add(
        ['docker', 'tag', image, targetImage].join(' ')
      )
    }
    commands.add(
      ['docker', 'push', targetImage].join(' ')
    )
    if ((options.registry || options.name) && !options.keepImage) {
      commands.add(
        ['docker', 'rmi', cmdOpt('no-prune', true), targetImage].join(' ')
      )
    }
    shell.execute(script: commands.join('\n'), envs)
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

  private String cmdOpt(String name, def value) {
    (name && value) ? "--${ name }=${ value }".toString() : ''
  }
}
