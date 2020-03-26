package com.e4d.file

import com.cloudbees.groovy.cps.NonCPS
import com.e4d.shell.Shell

class FindTool {
  final Shell shell

  FindTool(Shell shell) {
    this.shell = shell
  }

  List<String> find(Map options=[:]) {
    final words = [
      'find',
      options.under,
      cmdOpt('regextype', options.regexType),
      cmdExpr('regex', options.regex),
      cmdExpr('name', options.name),
    ]
    extractLines(
      shell.execute([],
        script: words.findAll().join(' '),
        returnStdout: true,
      )
    )
  }

  private String cmdOpt(String name, String value) {
    value ? "-${ name } ${ value }" : ''
  }

  private String cmdExpr(String kind, String term) {
    term ? "-${ kind } \'${ term }\'" : ''
  }

  @NonCPS
  private List<String> extractLines(String text) {
    (text?.split('\n') ?: []).findAll().toList()
  }
}
