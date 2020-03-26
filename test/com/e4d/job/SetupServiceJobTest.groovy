package com.e4d.job

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*
import com.e4d.git.GitSourceReference

class SetupServiceJobTest {
  final workflow = [:]
  final job = new SetupServiceJob(workflow)

  @Test void on_initialization_when_source_git_is_not_defined_it_gets_constructed_from_service_name() {
    job.serviceName = 'service name'
    job.sourceGit = null
    job.initialize()
    assertThat(job.sourceGit, hasProperty('repository', equalTo('service name')))
  }

  @Test void initialize_does_not_change_source_git_when_it_is_already_defined() {
    final reference  = new GitSourceReference('defined')
    job.serviceName = 'service name'
    job.sourceGit = reference
    job.initialize()
    assertThat(job.sourceGit, is(sameInstance(reference)))
  }

  @Test void on_initialization_when_service_name_is_not_defined_it_gets_from_source_git_repository() {
    job.serviceName = null
    job.sourceGit = new GitSourceReference(repository: 'repository')
    job.initialize()
    assertThat(job.serviceName, is(equalTo('repository')))
  }

  @Test void initialize_does_not_change_service_name_when_it_is_already_defined() {
    job.serviceName = 'service name'
    job.sourceGit = new GitSourceReference(repository: 'repository')
    job.initialize()
    assertThat(job.serviceName, is(equalTo('service name')))
  }
}
