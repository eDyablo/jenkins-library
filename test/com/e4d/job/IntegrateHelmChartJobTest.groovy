package com.e4d.job

import com.e4d.build.SemanticVersion
import com.e4d.build.SemanticVersionBuilder
import com.e4d.file.FileHub
import com.e4d.helm.HelmTool
import com.e4d.pipeline.DummyPipeline
import java.net.URI
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class IntegrateHelmChartJobNewlyCreatedTest {
  final pipeline = new DummyPipeline()
  final job = spy(new IntegrateHelmChartJob(pipeline))

  @Test void has_intialized_file_hub() {
    assertThat(job.fileHub, allOf(
      is(not(null)),
      hasProperty('shell', allOf(
        is(not(null)),
        hasProperty('pipeline', is(sameInstance(pipeline)))
      )),
    ))
  }

  @Test void has_initialized_helm() {
    assertThat(job.helm, allOf(
      is(not(null)),
      hasProperty('shell', allOf(
        is(not(null)),
        hasProperty('pipeline', is(sameInstance(pipeline)))
      )),
    ))
  }

  @Test void has_default_git_config() {
    final git = [:]
    git.with(DefaultValues.git)
    assertThat(job.gitConfig, allOf(
      hasProperty('baseUrl', equalTo(git.baseUrl)),
      hasProperty('credsId', equalTo(git.credsId)),
      hasProperty('branch', equalTo(git.branch)),
    ))
  }

  @Test void when_created_has_default_nexus_config() {
    final nexus = [:]
    nexus.with(DefaultValues.nexus)
    assertThat(job.nexusConfig, allOf(
      hasProperty('baseUrl', equalTo(nexus.baseUrl)),
      hasProperty('port', equalTo(nexus.port)),
      hasProperty('credsId', equalTo(nexus.credsId)),
      hasProperty('apiKey', equalTo(nexus.apiKey)),
    ))
  }
}

class IntegrateHelmChartJobTest {
  final pipeline = spy(new DummyPipeline())
  final job = spy(new IntegrateHelmChartJob(pipeline))
  final helm = mock(HelmTool)
  final fileHub = mock(FileHub)

  @Before void beforeEachTest() {
    job.helm = helm
    job.fileHub = fileHub
    doReturn([:]).when(job).checkoutSource(any(Map))
    when(helm.packageChart(any(Map))).thenReturn([:])
    doNothing().when(job).markStageSkipped(argThat(instanceOf(String)))
  }

  @Test void run_does_checkout_stage() {
    job.run()
    verify(pipeline).stage(eq('checkout'), any(Closure))
    verify(job).checkout()
  }

  @Test void when_load_parameters_keeps_git_config_branch_intact_if_sha1_is_not_set() {
    job.gitConfig.branch = 'intact'
    [
      null,
      [:],
      [sha1: null],
      [sha1: ''],
      [sha1: ' '],
    ].each { params ->
      job.loadParameters(params)
      assertThat("\n     For: ${ params }",
        job.gitConfig.branch, is(equalTo('intact')))
    }
  }

  @Test void checkout_sets_job_source() {
    doReturn([dir: 'source']).when(job).checkoutSource(any(Map))
    job.checkout()
    assertThat(job.source, is(equalTo([dir: 'source'])))
  }

  @Test void run_does_package_stage() {
    doReturn(chartMetadataFile: 'chart metadata').when(job).study()
    job.run()
    verify(pipeline).stage(eq('package'), any(Closure))
    verify(job).packageChart()
  }

  @Test void package_chart_builds_chart_package_from_source_dir() {
    job.source = [dir: 'source dir']
    job.packageChart()
    verify(helm).packageChart(argThat(hasEntry('chartPath', 'source dir')))
  }

  @Test void package_chart_sets_job_chart() {
    job.source = [:]
    when(helm.packageChart(any(Map))).thenReturn('chart')
    job.packageChart()
    assertThat(job.chart, is(equalTo('chart')))
  }

  @Test void run_does_testing_stage() {
    doReturn(chartMetadataFile: 'chart').when(job).study()
    job.run()
    verify(pipeline).stage(eq('test'), any(Closure))
    verify(job).testChart()
  }

  @Test void test_chart_runs_helm_lint_chart_for_job_source_dir() {
    job.source = [dir: 'source dir']
    job.testChart()
    verify(helm).lintChart(argThat(hasEntry('path', 'source dir')))
  }

  @Test void source_version_gets_constructed_from_source_tag() {
    job.source = [tag: 'v1.2.3-4-gdeadbee']
    final version = new SemanticVersionBuilder().fromGitTag(job.source.tag).build()
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
    doReturn(SemanticVersion.ZERO).when(job).sourceVersion
    assertThat(job.artifactVersion, is(SemanticVersion.ZERO))
  }

  @Test void artifact_version_is_equal_to_source_version_when_source_version_has_no_builds() {
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

  @Test void package_chart_sets_chart_version_equal_to_textual_representation_of_artifact_version() {
    job.source = [:]
    final version = new SemanticVersion(major: 1, minor: 2)
    doReturn(version).when(job).artifactVersion
    job.packageChart()
    verify(helm).packageChart(argThat(hasEntry('version', version.toString())))
  }

  @Test void run_publishes_chart_in_publish_stage() {
    job.chart = [path: 'chart path']
    job.run()
    verify(pipeline).stage(eq('publish'), any(Closure))
    verify(job).publishChart()
  }

  @Test void publish_chart_uploads_file_referenced_by_job_chart_path() {
    job.chart = [path: 'chart path']
    job.publishChart()
    verify(fileHub).uploadFile(argThat(hasEntry('file', 'chart path')))
  }

  @Test void publish_chart_uploads_file_to_file_hub_into_destination_specified_in_chart_repository_uri() {
    doReturn(new URI('chart-repository')).when(job).chartRepositoryURI
    job.chart = [:]
    job.publishChart()
    verify(fileHub).uploadFile(argThat(hasEntry(
      'destination', 'chart-repository')))
  }

  @Test void chart_repository_uri_gets_constructed_from_nexus_config_base_url() {
    job.nexusConfig.baseUrl = 'scheme://host:port/path'
    assertThat(job.chartRepositoryURI.toString(),
      is(equalTo('scheme://host:port/path/repository/charts/')))
  }

  @Test void chart_repository_uri_has_user_info_from_nexus_config_creds() {
    job.nexusConfig.credsId = 'nexus config creds'
    doReturn(['user', 'password']).when(job).getUsernamePassword('nexus config creds')
    assertThat(job.chartRepositoryURI.userInfo, is(equalTo('user:password')))
  }

  @Test void chart_repository_uri_has_no_user_info_in_its_textual_representation() {
    doReturn(['user', 'password']).when(job).getUsernamePassword(any(String))
    assertThat(job.chartRepositoryURI,
      hasToString(allOf(
        not(containsString('user')),
        not(containsString('password')),
      ))
    )
  }

  @Test void publish_chart_uploads_file_using_user_and_password_from_chart_repository_uri_user_info() {
    final uri = new URI()
    uri.userInfo = 'user:password'
    doReturn(uri).when(job).chartRepositoryURI
    job.chart = [:]
    job.publishChart()
    verify(fileHub).uploadFile(argThat(
      allOf(
        hasEntry('user', 'user'),
        hasEntry('password', 'password'),
      )
    ))
  }

  @Test void deploy_chart_triggers_deploy_job_with_chart_file_name_as_parameter_named_helm_chart() {
    job.chart = [path: 'path/to/chart']
    job.deployChart()
    verify(pipeline).build(argThat(
      hasEntry(
        equalTo('parameters'),
        hasItem(hasToString(containsString("helmChart='chart'")))
      )
    ))
  }

  @Test void package_chart_updates_chart_dependencies() {
    job.source = [:]
    job.packageChart()
    verify(helm).packageChart(
      argThat(
        hasEntry('updateDependencies', true)
      )
    )
  }
}
