package com.e4d.declaration

import org.jenkinsci.plugins.ghprb.extensions.comments.GhprbBuildResultMessage
import org.jenkinsci.plugins.ghprb.extensions.status.GhprbSimpleStatus

class GitHubPullRequestBuilderDeclaration extends Declaration {
  final Map options = [
    orgsList: [],
    extensions: [],
  ]

  void organizations(String[] orgs) {
    orgs.collect(options.orgsList) { it }
  }

  void updateCommitStatus(Map args) {
    options.extensions.add(new GhprbSimpleStatus(
      args.showMatrix ?: false,
      args.context ?: '',
      args.url ?: '',
      args.triggered ?: '',
      args.started ?: '',
      args.addTestResults ?: false,
      (args.completed ?: []) as List<GhprbBuildResultMessage>
    ))
  }
}
