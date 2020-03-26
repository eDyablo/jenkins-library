package com.e4d.build

import com.e4d.build.*
import com.e4d.git.GitConfig
import com.e4d.git.GitSourceReference
import com.e4d.job.*
import com.e4d.pipeline.*
import com.e4d.step.*
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class IntegrateJobTest {
  final pipeline = mock(DummyPipeline)
  final job = spy(new IntegrateJob(pipeline))

  @Before void beforeEachTest() {
    reset(job)
    doReturn([:]).when(job).checkoutSource(argThat(any(Map)))
  }

  @Test void source_version_gets_constructed_from_source_tag() {
    final gitInfo = [tag: 'v1.2.3-4-gdeadbee']
    job.source = gitInfo
    final version = new SemanticVersionBuilder().fromGitTag(gitInfo.tag).build()
    assertThat(job.sourceVersion, is(equalTo(version)))
  }

  @Test void source_version_is_zero_when_no_source() {
    job.source = null
    assertThat(job.sourceVersion, is(SemanticVersion.ZERO))
  }

  @Test void source_version_is_zero_when_source_has_no_tag() {
    job.source = [tag: null]
    assertThat(job.sourceVersion, is(SemanticVersion.ZERO))
  }

  @Test void artifact_version_is_zero_when_source_version_is_zero() {
    job.source = null
    doReturn(SemanticVersion.ZERO).when(job).sourceVersion
    assertThat(job.artifactVersion, is(SemanticVersion.ZERO))
  }

  @Test void artifact_version_is_equal_to_source_version_when_source_version_has_no_builds() {
    job.source = null
    final sourceVersion = new SemanticVersion(major: 1, minor: 2, patch: 3, prerelease: ['a', 1])
    doReturn(sourceVersion).when(job).sourceVersion
    assertThat(job.artifactVersion, is(equalTo(sourceVersion)))
  }

  @Test void artifact_version_contains_source_unix_timestamp_as_additional_prerelease_when_source_version_has_build() {
    job.source = [timestamp: 1234567]
    final sourceVersion = new SemanticVersion(major: 1, minor: 2, patch: 3,
      prerelease: ['a', 1], build: 'b')
    final expectedVersion = new SemanticVersion(major: 1, minor: 2, patch: 3,
      prerelease: ['a', 1, 'sut', 1234567], build: 'b')
    doReturn(sourceVersion).when(job).sourceVersion
    assertThat(job.artifactVersion, is(equalTo(expectedVersion)))
  }

  @Test void artifact_version_is_equal_to_source_version_when_source_version_has_build_and_source_has_no_timestamp() {
    job.source = [timestamp: null]
    final sourceVersion = new SemanticVersion(major: 1, minor: 2, patch: 3,
      prerelease: ['a', 1], build: 'b')
    doReturn(sourceVersion).when(job).sourceVersion
    assertThat(job.artifactVersion, is(equalTo(sourceVersion)))
  }

  @Test void load_parameters_set_git_config_branch_to_sha1_when_it_is_defined() {
    // Arrange
    job.gitConfig.branch = 'branch'
    // Act
    job.loadParameters([sha1: 'sha1'])
    // Assert
    assertThat(job.gitConfig.branch, is(equalTo('sha1')))
  }

  @Test void load_parameters_leaves_git_config_intact_when_no_sha1_defined() {
    [
      null,
      [:],
      [sha1: null],
      [sha1: ''],
      [sha1: ' '],
    ].each { params ->
      // Arrange
      job.gitConfig.branch = 'intact'
      // Act
      job.loadParameters(params)
      // Assert
      assertThat("\n     For: ${ params }",
        job.gitConfig.branch, is(equalTo('intact')))
    }
  }

  @Test void initialize_job_resolves_git_source_reference_against_git_config() {
    [
      'Take owner from config when reference has no owner',
      new GitConfig(owner: 'owner'),
      new GitSourceReference(repository: 'repository'),
      new GitSourceReference(owner: 'owner', repository: 'repository'),

      'Do not take owner from config when reference has owner',
      new GitConfig(owner: 'config'),
      new GitSourceReference(owner: 'reference'),
      new GitSourceReference(owner: 'reference'),

      'Take host from config when reference has no host',
      new GitConfig(host: 'host'),
      new GitSourceReference(owner: 'owner'),
      new GitSourceReference(host: 'host', owner: 'owner'),

      'Do not take host from config when reference has host',
      new GitConfig(host: 'config'),
      new GitSourceReference(host: 'reference', owner: 'owner'),
      new GitSourceReference(host: 'reference', owner: 'owner'),

      'Take scheme and host from config when reference has no host',
      new GitConfig(scheme: 'scheme', host: 'host'),
      new GitSourceReference(owner: 'owner'),
      new GitSourceReference(scheme: 'scheme', host: 'host', owner: 'owner'),

      'Do not take scheme from config when reference has scheme and has host',
      new GitConfig(scheme: 'config'),
      new GitSourceReference(scheme: 'reference', host: 'host'),
      new GitSourceReference(scheme: 'reference', host: 'host'),

      'Take scheme and host from config when reference has scheme but has no host',
      new GitConfig(scheme: 'config', host: 'host'),
      new GitSourceReference(scheme: 'reference'),
      new GitSourceReference(scheme: 'config', host: 'host'),

      'Do not take scheme from config when config and reference have no hosts',
      new GitConfig(scheme: 'config'),
      new GitSourceReference(scheme: 'reference'),
      new GitSourceReference(scheme: 'reference'),

      'Take branch from config when reference has no branch',
      new GitConfig(branch: 'branch'),
      new GitSourceReference(),
      new GitSourceReference(branch: 'branch'),

      'Do not take branch from config when reference has branch',
      new GitConfig(branch: 'config'),
      new GitSourceReference(branch: 'reference'),
      new GitSourceReference(branch: 'reference'),
    ]
    .collate(4)
    .each { test, config, reference, expected ->
      // Arrange
      job.gitConfig = config
      job.gitSourceRef = reference
      // Act
      job.initializeJob()
      // Assert
      assertThat("\n     For: ${test}",
        job.gitSourceRef, is(equalTo(expected)))
    }
  }

  @Test void checkout_checks_out_source_specified_by_git_source_reference() {
    // Arrange
    job.gitSourceRef = new GitSourceReference(
      host: 'host',
      owner: 'owner',
      repository: 'repository',
      branch: 'branch',
    )
    // Act
    job.checkout()
    // Assert
    verify(job).checkoutSource(argThat(
      allOf(
        hasEntry('baseUrl', 'host/owner'),
        hasEntry('repository', 'repository'),
        hasEntry('branch', 'branch'),
      )
    ))
  }

  @Test void checkout_uses_creds_specified_in_git_config() {
    // Arrange
    job.gitConfig = new GitConfig(credsId: 'creds')
    // Act
    job.checkout()
    // Assert
    verify(job).checkoutSource(argThat(
      hasEntry('credsId', 'creds'),
    ))
  }

  @Test void checkout_uses_job_pipeline() {
    // Act
    job.checkout()
    // Assert
    verify(job).checkoutSource(argThat(
      hasEntry(equalTo('pipeline'), is(sameInstance(pipeline))),
    ))
  }

  @Test void checkout_returns_source_dir_combined_from_checkedout_source_and_source_reference() {
    // Arrange
    doReturn(dir: 'root').when(job).checkoutSource(argThat(instanceOf(Map)))
    job.gitSourceRef = new GitSourceReference(directory: 'dir')
    // Act & Assert
    assertThat(job.checkout(), hasEntry('dir', 'root/dir'))
  }
}
