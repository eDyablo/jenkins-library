package com.e4d.job

import com.e4d.build.SemanticVersion
import com.e4d.build.SemanticVersionBuilder
import com.e4d.git.GitConfig
import com.e4d.git.GitSourceReference
import com.e4d.step.CheckoutRecentSourceStep

class IntegrateJob extends PipelineJob {
  boolean publishPrereleaseVersion = true
  def source
  GitConfig gitConfig = new GitConfig()
  GitSourceReference gitSourceRef = new GitSourceReference()
  String artifactBaseName

  IntegrateJob(pipeline) {
    super(pipeline)
    gitConfig.with(DefaultValues.git)
  }

  def loadParameters(params) {
    if (params?.sha1?.trim()) {
      gitSourceRef = gitSourceRef.withBranch(params.sha1.trim())
    }
  }

  void initializeJob() {
    gitSourceRef = resolveSourceReference()
  }

  def resolveSourceReference() {
    new GitSourceReference(
      scheme: gitSourceRef.host
        ? gitSourceRef.scheme
        : (gitConfig.host ? gitConfig.protocol : gitSourceRef.scheme),
      host: gitSourceRef.host ?: gitConfig.host,
      owner: gitSourceRef.owner ?: gitConfig.owner,
      repository: gitSourceRef.repository,
      branch: gitSourceRef.branch ?: gitConfig.branch,
      directory: gitSourceRef.directory,
    )
  }

  def checkout() {
    source = checkoutSource(
      baseUrl: gitSourceRef.organizationUrl,
      repository: gitSourceRef.repository,
      branch: gitSourceRef.branch,
      credsId: gitConfig.credsId,
      pipeline: pipeline,
    )
    source += [
      dir: [source.dir, gitSourceRef.directory].findAll().join('/')
    ].findAll { key, value -> value }
  }

  def checkoutSource(Map options) {
    CheckoutRecentSourceStep.newInstance(options).run()
  }

  SemanticVersion getSourceVersion() {
    new SemanticVersionBuilder().fromGitTag(source?.tag).build()
  }

  SemanticVersion getArtifactVersion() {
    def version = sourceVersion
    if (version.build) {
      final builder = new SemanticVersionBuilder()
        .major(version.major)
        .minor(version.minor)
        .patch(version.patch)
        .prerelease(version.prereleaseIds)
      if (source.timestamp) {
        builder.prerelease('sut', source.timestamp)
      }
      builder.build(version.buildIds)
      version = builder.build()
    }
    return version
  }

  def declare(Closure definition) {
    definition.delegate = new declaration.IntegrateJobDeclaration(this)
    definition.resolveStrategy = Closure.DELEGATE_ONLY
    definition.call()
  }
}
