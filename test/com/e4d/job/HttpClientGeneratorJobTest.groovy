package com.e4d.job

import com.e4d.build.*
import com.e4d.git.GitSourceReference
import com.e4d.nexus.*
import com.e4d.nuget.NugetRepository
import com.e4d.pipeline.DummyPipeline
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*

class HttpClientGeneratorJobTest {
  final pipeline = spy(DummyPipeline)

  final job = spy(new HttpClientGeneratorJob(pipeline))

  @Before void beforeEachTest() {
    job.gitSourceRef = new GitSourceReference(repository: 'svc-test-service')
  }

  private void initializeGenerateHttpClientTest(language, findSwagger=true) {
    def values = [
      language: language
    ]
    def sourceDir = '/foo/bar'
    def artifactVersion = new SemanticVersion(major: 1, minor: 0, patch: 0)
    def source = [
      dir: sourceDir
    ]

    when(job.artifactVersion).thenReturn(artifactVersion)

    if (findSwagger) {
      when(pipeline.sh(script:"find ${ sourceDir }/src -type f -name swagger.json", returnStdout: true)).thenReturn(
        "${ sourceDir }/swagger.json"
      )
    }
    else {
      when(pipeline.sh(script:"find ${ sourceDir }/src -type f -name swagger.json", returnStdout: true)).thenReturn(
        ""
      )
    }

    job.values = values
    job.source = source
  }

  private void initializePublishHttpClientTest(language, findSwagger=true) {
    def values = [
      language: language
    ]
    def sourceDir = '/foo/bar'
    def source = [
      dir: sourceDir
    ]

    job.values = values
    job.source = source
  }

  @Test void newly_created_has_default_nexus_config() {
    final def nexus = [:]
    def job = new HttpClientGeneratorJob(pipeline)
    nexus.with(DefaultValues.nexus)
    assertThat(job.nexusConfig, allOf(
      hasProperty('baseUrl', equalTo(nexus.baseUrl)),
      hasProperty('port', equalTo(nexus.port)),
      hasProperty('credsId', equalTo(nexus.credsId)),
      hasProperty('apiKey', equalTo(nexus.apiKey)),
    ))
  }

  @Test void newly_created_has_default_git_config() {
    final def git = [:]
    git.with(DefaultValues.git)
    assertThat(job.gitConfig, allOf(
      hasProperty('baseUrl', equalTo(git.baseUrl)),
    ))
  }

  @Test void generateHttpClient_python_calls_right_options() {
    initializeGenerateHttpClientTest('python')
    job.generateHttpClient()
    verify(pipeline).sh(script: "java -jar ${ job.generatorJar } generate -g python -i /foo/bar/swagger.json -o /foo/bar/httpClient "
        + "--additional-properties packageName=test_service_api_client,projectName=test-service-api-client,packageVersion=1.0.0")
  }

  @Test void generateHttpClient_unsupported_language_pipeline_error() {
    initializeGenerateHttpClientTest('assembly')
    job.generateHttpClient()
    verify(pipeline, atLeast(1)).error(any())
  }

  @Test void generateHttpClient_no_swagger_pipeline_error() {
    initializeGenerateHttpClientTest('python', false)
    job.generateHttpClient()
    verify(pipeline, atLeast(1)).error(any())
  }

  @Test void publishHttpClient_python_success() {
    initializePublishHttpClientTest('python')

    job.publishHttpClient()
    verify(pipeline).dir(any(), any())
  }

  @Test void publishHttpClient_unsupported_language_pipeline_error() {
    initializePublishHttpClientTest('assembly')

    job.publishHttpClient()
    verify(pipeline).error(any())
  }

  @Test void setting_source_git_sets_git_source_reference() {
    job.sourceGit = 'host/owner/repository/directory'
    assertThat(job.gitSourceRef, is(equalTo(
      new GitSourceReference('host/owner/repository/directory'))))
  }
}
