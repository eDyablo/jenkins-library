package com.e4d.curl

import com.e4d.build.CommandLineBuilder

class CurlTool {
  final CommandLineBuilder commandLineBuilder
  final String tool = 'curl'
  final def script

  String password
  String user

  CurlTool(def script) {
    this.script = script
    commandLineBuilder = new CommandLineBuilder(
      optionKeyValueSeparator: ' ')
  }

  def uploadFile(String file, String url) {
    uploadFile(file: file, url: url)
  }

  def uploadFile(Map args) {
    def user = args.user ?: this.user
    def password = args.password ?: this.password
    def command = commandLineBuilder.buildCommand(
      [tool], [args.url],
      user: [user, password].join(':'),
      upload_file: args.file)
    script.sh(command)
  }

  def downloadFile(String url, String file) {
    downloadFile(url: url, file: file)
  }

  def downloadFile(Map args) {
    def user = args.user ?: this.user
    def password = args.password ?: this.password
    def command = commandLineBuilder.buildCommand(
      [tool], [args.url],
      user: [user, password].join(':'),
      output: args.file)
    script.sh(command)
  }
}
