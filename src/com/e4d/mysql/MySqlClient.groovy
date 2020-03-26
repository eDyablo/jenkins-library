package com.e4d.mysql

class MySqlClient {
  def script
  String host
  String user
  String password

  MySqlClient(def script, String host, String user, String password) {
    this.script = script
    this.host = host
    this.user = user
    this.password = password
  }

  String execute(String clause) {
    String output = script.sh(
      script: "set +x; mysql --host='${host}' --user='${user}' --password='${password}' --execute='${clause}'; set -x",
      returnStdout: true,
      encoding: 'utf-8')
    return output.trim()
  }
}
