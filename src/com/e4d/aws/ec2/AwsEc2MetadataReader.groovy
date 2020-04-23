package com.e4d.aws.ec2

import com.e4d.shell.Shell
import groovy.json.JsonSlurperClassic

class AwsEc2MetadataReader {
  final Shell shell
  final String serviceEndpoint = 'http://169.254.169.254'

  AwsEc2MetadataReader(Shell shell) {
    this.shell = shell
  }

  String readIamRole() {
    final endpoint = [
      serviceEndpoint,
      'latest', 'meta-data', 'iam', 'security-credentials'
    ].join('/')
    shell.execute([],
      script: "curl --silent ${ endpoint }",
      returnStdout: true
    )
  }

  Map readSecurityCredentials(String role='') {
    final credsEndpoint = [
      serviceEndpoint,
      'latest', 'meta-data', 'iam', 'security-credentials'
    ].join('/')
    final commands = [
      'set -o errexit',
    ]
    if (!role) {
      commands.add("role=\$(curl --silent ${ credsEndpoint })")
      role = '${role}'
    }
    commands.add("curl --silent \"${ credsEndpoint }/${role}\"")
    final String output = shell.execute([],
      script: commands.findAll().join('\n'),
      returnStdout: true
    )
    JsonSlurperClassic.newInstance().parseText(output ?: '{}')
  }
}
