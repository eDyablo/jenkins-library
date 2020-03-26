package com.e4d.job

import com.e4d.build.TextValue

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class TestServiceJobTest {
  final pipeline = [:]
  final job = new TestServiceJob(pipeline)

  @Test void newly_created_has_default_nexus_config() {
    final nexus = [:]
    nexus.with(DefaultValues.nexus)
    assertThat(job.nexusConfig, allOf(
      hasProperty('baseUrl', equalTo(nexus.baseUrl)),
      hasProperty('port', equalTo(nexus.port)),
      hasProperty('credsId', equalTo(nexus.credsId)),
      hasProperty('apiKey', equalTo(nexus.apiKey)),
    ))
  }

  @Test void newly_created_has_default_k8s_config() {
    final secret = [:]
    secret.with(DefaultValues.k8sConfigSecret)
    assertThat(job.k8sConfig, allOf(
      hasProperty('configRef', allOf(
        hasProperty('name', equalTo(secret.name)),
        hasProperty('key', equalTo(secret.key)),
      )),
    ))
  }

  @Test void initialize_job_sets_k8s_context_to_service_environment_context() {
    job.service = 'service : context'
    job.initializeJob()
    assertThat(job.k8s.context, is(equalTo('context')))
  }
}
