package com.e4d.job

import com.e4d.build.ArtifactReference
import com.e4d.build.SemanticVersion
import com.e4d.build.SemanticVersionBuilder
import com.e4d.build.TextValue
import com.e4d.k8s.K8sClient
import com.e4d.k8s.K8sConfig
import com.e4d.nexus.NexusConfig
import com.e4d.nuget.NugetRepository
import com.e4d.pipeline.DummyPipeline

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class DeployServiceJobTest {
  final def pipeline = spy(DummyPipeline)
  def job = spy(new DeployServiceJob(pipeline))

  @Test void newly_created_has_default_nexus_config() {
    final def nexus = [:]
    nexus.with(DefaultValues.nexus)
    assertThat(job.nexusConfig, allOf(
      hasProperty('baseUrl', equalTo(nexus.baseUrl)),
      hasProperty('port', equalTo(nexus.port)),
      hasProperty('credsId', equalTo(nexus.credsId)),
      hasProperty('apiKey', equalTo(nexus.apiKey)),
    ))
  }
  @Test void newly_created_has_default_k8s_config_reference() {
    final def secret = [:]
    secret.with(DefaultValues.k8sConfigSecret)
    assertThat(job.k8sConfigRef, allOf(
      hasProperty('name', equalTo(secret.name)),
      hasProperty('key', equalTo(secret.key)),
    ))
  }

  @Test void newly_created_has_empty_artifact_data() {
    assertThat(job.artifactData, is([:]))
  }

  @Test void reads_artifact_data_from_artifact_data_json_file_when_it_exists() {
    when(pipeline.fileExists('.ci/artifact-data')).thenReturn(true)
    when(pipeline.readFile('.ci/artifact-data')).thenReturn('''\
      {
        "artifact": {
          "data": {
            "value": 1
          }
        }
      }
    '''.stripIndent())
    assertThat(job.artifactData, is([
      artifact: [
        data: [
          value: 1,
        ],
      ],
    ]))
  }

  @Test void has_empty_artifact_data_when_artifact_data_file_does_not_exist() {
    when(pipeline.readFile('.ci/artifact-data')).thenReturn('{"artifact":"data"}')
    when(pipeline.fileExists('.ci/artifact-data')).thenReturn(false)
    assertThat(job.artifactData, is([:]))
  }

  @Test void get_ingress_url_obtained_from_k8s() {
    final k8s = mock(K8sClient)
    job.k8s = k8s
    doReturn('ingress url').when(k8s).getResourceData(
      any(Map), argThat(equalTo('service')), argThat(equalTo(['e4d-rproxy'])))
    assertThat(job.ingressUrl, is(equalTo('ingress url')))
  }

  @Test void ingress_url_is_empty_when_k8s_responds_with_no_value() {
    final k8s = mock(K8sClient)
    job.k8s = k8s
    doReturn('<no value>').when(k8s).getResourceData(
      any(Map), argThat(equalTo('service')), any(List))
    assertThat(job.ingressUrl, is(equalTo('')))
  }

  @Test void get_service_host_returns_host_obtained_from_k8s_when_no_ingress_url() {
    final k8s = mock(K8sClient)
    job.k8s = k8s
    doReturn('').when(job).ingressUrl
    doReturn([url: 'url', creds: 'creds']).when(k8s).getServer('context')
    job.destination.context = 'context'
    assertThat(job.serviceHost, allOf(
      hasEntry('url', 'url'),
      hasEntry('creds', 'creds'),
    ))
  }

  @Test void get_service_host_returns_host_with_url_equal_to_ingress_and_with_none_creds_when_ingress_is_present() {
    final k8s = mock(K8sClient)
    job.k8s = k8s
    doReturn('ingress').when(job).ingressUrl
    doReturn([url: 'url', creds: 'creds']).when(k8s).getServer('context')
    job.destination.context = 'context'
    assertThat(job.serviceHost, allOf(
      hasEntry('url', 'ingress'),
      hasEntry('creds', 'none'),
    ))
  }

  @Test void get_service_url_returns_service_self_link_postfixed_with_http_proxy_when_no_ingress() {
    doReturn('').when(job).ingressUrl
    doReturn('service').when(job).serviceSelfLink
    assertThat(job.serviceUrl, is(equalTo('service:http/proxy')))
  }

  @Test void properly_constructs_service_end_point_from_service_host_url_and_service_url() {
    [
      [url: 'server'],
      'service',
      'server/service',

      [url: 'server/'],
      'service',
      'server/service',

      [url: 'server'],
      '/service',
      'server/service',
    ]
    .collate(3)
    .each { host, service, expected ->
      doReturn(host).when(job).serviceHost
      doReturn(service).when(job).serviceUrl
      assertThat(job.serviceEndpoint, is(equalTo(expected)))
    }
  }

  @Test void service_url_gets_constructed_from_artifact_name_and_destination_namespace_when_ingress_is_present() {
    doReturn('ingress').when(job).ingressUrl
    job.artifact = 'artifact:v1.0.0'
    job.destination.namespace = 'namespace'
    assertThat(job.serviceUrl, is(equalTo('/namespace/artifact')))
  }
}

class DeployServiceJobSourceVersionTest {
  final def job = mock(DeployServiceJob)

  @Before void beforeEachTest() {
    when(job.sourceVersion).thenCallRealMethod()
  }

  @Test void has_source_version_build_from_artifact_data_source_snapshot_tag() {
    def tests = [
      'v1',
      'v1.2',
      'v1.2.3',
      'v1.2.3-gdeadbee',
    ]
    tests.each { tag ->
      when(job.artifactData).thenReturn([ source: [ snapshot: [ tag: tag ] ] ])
      final def expected = new SemanticVersionBuilder().fromGitTag(tag).build()
      assertThat("\n     For: ${ tag }", job.sourceVersion, is(expected))
    }
  }

  @Test void has_zero_source_version_when_it_has_neiser_artifact_data_nor_artifact_reference() {
    when(job.artifactData).thenReturn([:])
    when(job.artifact).thenReturn(null)
    assertThat(job.sourceVersion, equalTo(SemanticVersion.ZERO))
  }

  @Test void has_zero_source_version_when_artifact_data_has_neiser_source_snapshot_tag_nor_arifact_reference() {
    final def tests = [
      [source: null],
      [source: [:]],
      [source: [snapshot: null]],
      [source: [snapshot: [:]]],
      [source: [snapshot: [tag: null]]],
      [source: [snapshot: [tag: '']]],
    ]
    when(job.artifact).thenReturn(null)
    tests.each { data ->
      when(job.artifactData).thenReturn(data)
      assertThat("\n     For: ${ data }",
        job.sourceVersion, equalTo(SemanticVersion.ZERO))
    }
  }

  @Test void has_source_version_get_from_artifact_reference_tag_when_artifact_data_has_no_source_snapshot_tag() {
    final def tests = [
      [source: null],
      [source: [:]],
      [source: [snapshot: null]],
      [source: [snapshot: [:]]],
      [source: [snapshot: [tag: null]]],
      [source: [snapshot: [tag: '']]],
    ]
    final def artifactReference = new ArtifactReference('', 'v1.2.3-4-gdeadbee')
    final def version = new SemanticVersionBuilder().fromGitTag(artifactReference.tag).build()
    when(job.artifact).thenReturn(artifactReference)
    tests.each { data ->
      when(job.artifactData).thenReturn(data)
      assertThat("\n     For: ${ data }", job.sourceVersion, equalTo(version))
    }
  }
}

class DeployServiceJobArtifactVersionTest
{
  final def job = mock(DeployServiceJob)

  @Before void beforeEachTest() {
    when(job.artifactVersion).thenCallRealMethod()
  }

  @Test void has_artifact_version_equal_to_source_version_when_source_version_has_no_build_id() {
    final def tests = [
      new SemanticVersion(major: 1),
      new SemanticVersion(minor: 1, prerelease: 'prerelease'),
      new SemanticVersion(patch: 1, prerelease: ['first', 'second']),
    ]
    when(job.artifactData).thenReturn([source: [snapshot: [timestamp: 'timestamp' ]]])
    tests.each { sourceVersion ->
      when(job.sourceVersion).thenReturn(sourceVersion)
      assertThat("\n     For: ${ sourceVersion }",
        job.artifactVersion, equalTo(sourceVersion))
    }
  }

  @Test void has_artifact_version_with_source_unix_time_as_its_last_prerelease_when_source_version_has_any_build() {
    final def tests = [
      [ new SemanticVersion(major: 1, build: 'build'),
        new SemanticVersion(major: 1, prerelease: ['sut', 'timestamp'], build: 'build') ],

      [ new SemanticVersion(minor: 1, build: 'build', prerelease: 'prerelease'),
        new SemanticVersion(minor: 1, build: 'build', prerelease: ['prerelease', 'sut', 'timestamp']) ],

      [ new SemanticVersion(patch: 1, build: ['first', 'second'], prerelease: 'prerelease'),
        new SemanticVersion(patch: 1, build: ['first', 'second'], prerelease: ['prerelease', 'sut', 'timestamp']) ],
    ]
    when(job.artifactData).thenReturn([source: [snapshot: [timestamp: 'timestamp' ]]])
    tests.each { sourceVersion, artifactVersion ->
      when(job.sourceVersion).thenReturn(sourceVersion)
      assertThat("\n     For: ${ sourceVersion }",
        job.artifactVersion, equalTo(artifactVersion))
    }
  }

  @Test void has_artifact_version_to_source_version_when_the_version_has_a_build_but_no_source_or_snapshot_or_timestamp_is_available() {
    final sourceVersion = new SemanticVersion(major: 1, minor: 2, patch: 3, build: ['b', 4])
    when(job.sourceVersion).thenReturn(sourceVersion)
    final artifacts = [
      [source: null],
      [source: []],
      [source: [snapshot: null]],
      [source: [snapshot: []]],
      [source: [snapshot: [timestamp: null]]],
    ]
    artifacts.each { artifact ->
      when(job.artifactData).thenReturn(artifact)
      assertThat(job.artifactVersion, is(equalTo(sourceVersion)))
    }
  }
}

class DeployServiceJobDeploymentContextTest {
  final DeployServiceJob job = mock(DeployServiceJob)
  final def pipeline = mock(DummyPipeline, CALLS_REAL_METHODS)
  final NexusConfig nexusConfig = mock(NexusConfig)

  @Before void beforeEachTest() {
    when(job.createDeploymentContext()).thenCallRealMethod()
    when(job.pipeline).thenReturn(pipeline)
    when(job.nexusConfig).thenReturn(nexusConfig)
  }

  @Test void created_context_has_artifact_version_from_the_job() {
    final version = new SemanticVersion(major: 1)
    when(job.artifactVersion).thenReturn(version)
    final context = job.createDeploymentContext()
    assertThat(context.artifactVersion, equalTo(version))
  }

  @Test void created_context_has_nuget_repository_from_the_job() {
    final repository = mock(NugetRepository)
    when(job.nugetRepository).thenReturn(repository)
    final context = job.createDeploymentContext()
    assertThat(context.nugetRepository, is(sameInstance(repository)))
  }
}

class DeployServiceJobNexusClientTest {
  final jobPipeline = mock(DummyPipeline, CALLS_REAL_METHODS)
  final job = new DeployServiceJob(jobPipeline)

  @Test void client_uses_pipeline_from_job() {
    assertThat(job.nexusClient.pipeline, is(sameInstance(jobPipeline)))
  }

  @Test void client_uses_nexus_credentials() {
    job.nexusConfig.baseUrl = 'base url'
    when(jobPipeline.env).thenReturn([user: 'user', password: 'password'])
    assertThat(job.nexusClient, allOf(
      hasProperty('baseUrl', equalTo('base url')),
      hasProperty('user', equalTo('user')),
      hasProperty('password', equalTo('password')),
    ))
  }
}

class DeployServiceJobInitializationTest {
  final job = new DeployServiceJob(mock(DummyPipeline))

  @Test void initialize_job_sets_config_path_for_k8s_equal_to_its_k8s_config_path() {
    job.k8sConfigRef.with {
      name = 'config'
      key = 'path'
    }
    job.initializeJob()
    assertThat(job.k8s.configPath, is(equalTo(job.k8sConfig.configPath)))
  }
}
