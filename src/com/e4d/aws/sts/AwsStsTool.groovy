package com.e4d.aws.sts

import com.e4d.build.CommandLineBuilder
import com.e4d.shell.Shell
import groovy.json.JsonSlurperClassic

class AwsStsTool {
  final CommandLineBuilder cmdBuilder
  final Shell shell

  AwsStsTool(Shell shell) {
    this.cmdBuilder = new CommandLineBuilder()
    this.shell = shell
  }

  def assumeRole(Map options=[:], String roleArn, String roleSession) {
    final def commands = [
      '#!/usr/bin/env bash',
      'set -o errexit',
      cmdBuilder.buildCommand(['aws', 'sts', 'assume-role'], [],
        options + [role_arn: roleArn, role_session_name: roleSession,
        output: 'json']),
    ]
    final String output = shell.execute([], script: commands.join('\n'),
      returnStdout: true, encoding: 'utf-8')
    return JsonSlurperClassic.newInstance().parseText(output)
  }
}
