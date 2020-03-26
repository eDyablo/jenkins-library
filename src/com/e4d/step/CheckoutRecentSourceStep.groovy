package com.e4d.step

import com.e4d.ioc.ContextRegistry
import groovy.json.JsonSlurperClassic

/**
 * Step that checks a repository's recent source out.
 */
class CheckoutRecentSourceStep implements Serializable {
  final def pipeline
  final String repository
  final String baseUrl
  final String branch
  final String credsId

  /**
   * Constructor
   *
   * @param options A map of arbitrary options.
   *        options.repository The name of a repository.
   *        optisons.baseUrl A string contains git URL that refers the repository.
   *        options.credsId A string contains id of credentials stored in Jenkins's
   *                        holding credentials to get access to the repository.
   *        options.branch A string denoting branch to get sources from.
   */
  CheckoutRecentSourceStep(Map options) {
    pipeline = options.pipeline ?: ContextRegistry.context.pipeline
    repository = options.repository
    baseUrl = options.baseUrl ?: pipeline.params.GIT_BASE_URL
    branch = options.branch ?: pipeline.params.GIT_BRANCH
    credsId = options.credsId ?: pipeline.params.GIT_CREDS_ID
  }

  /**
   * Runs the step.
   *
   * @return An object holding information about
   *         the checked out source.
   */
  def run() {
    pipeline.dir(repository) {
      final def currentDir = pipeline.pwd()
      final def checkout = pipeline.checkout(
        $class: 'GitSCM',
        branches: [[name: branch]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [],
        submoduleCfg: [],
        userRemoteConfigs: [[
          credentialsId: credsId,
          url: "${ baseUrl }/${ repository }",
          refspec: '+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*'
        ]]
      )
      final def description = describeCheckout(checkout)
      pipeline.withCredentials([pipeline.usernamePassword(
        credentialsId: credsId,
        usernameVariable: 'user',
        passwordVariable: 'password')]) {
        return [
          branch: checkout.GIT_BRANCH,
          changedFiles: description.diff?.files ?: [],
          commit: checkout.GIT_COMMIT,
          dir: currentDir,
          hash: description.hash,
          password: pipeline.env.password,
          revision: description.revision,
          tag: description.tag,
          timestamp: description.timestamp,
          url: checkout.GIT_URL,
          user: pipeline.env.user,
        ]
      }
    }
  }

  def describeCheckout(checkout) {
    pipeline.withEnv(["COMMIT=${ findTargetRevision(checkout) }"]) {
      final def output = pipeline.sh(
        script: pipeline.libraryResource('com/e4d/git/git-describe-checkout.sh'),
        returnStdout: true,
        encoding: 'utf-8')
      new JsonSlurperClassic().parseText(output)
    }
  }

  def findTargetRevision(checkout) {
    if (pipeline.params.ghprbTargetBranch) {
      [ extractRemote(checkout.GIT_BRANCH), pipeline.params.ghprbTargetBranch ]
        .findAll{ it }.join('/')
    } else {
      checkout.GIT_PREVIOUS_SUCCESSFUL_COMMIT ?: ''
    }
  }

  def extractRemote(branch) {
    if (branch) {
      final def parts = branch.split('/', 2)
      if (parts.length > 1) {
        parts.first()
      }
      else {
        'origin'
      }
    } else {
      branch
    }
  }
}
