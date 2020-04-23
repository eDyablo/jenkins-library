package com.e4d.job

import com.e4d.pipeline.DummyPipeline
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class FindWorkflowJobJobTest {
  final workflow = spy(DummyPipeline)
  final job = spy(new FindWorkflowJobJob(workflow))
  final parameters = [:]

  @Before void beforeEachTest() {
    doReturn(parameters).when(workflow).params
  }

  @Test void can_run_by_job_runner() {
    JobRunner.run(job)
  }

  @Test void load_parameters_changes_full_name_regex_when_it_is_defined_in_parameters() {
    parameters['full name regex'] = 'changed'
    job.fullNameRegex = 'intact'
    job.loadParameters()
    assertThat(job.fullNameRegex, is('changed'))
  }

  @Test void load_parameters_leaves_full_name_regex_intact_when_it_is_not_defined_in_parameters() {
    parameters['full name regex'] = null
    job.fullNameRegex = 'intact'
    job.loadParameters()
    assertThat(job.fullNameRegex, is('intact'))
  }

  @Test void load_parameters_changes_full_name_regex_when_it_is_defined_in_parameters_and_is_empty() {
    parameters['full name regex'] = ''
    job.fullNameRegex = 'intact'
    job.loadParameters()
    assertThat(job.fullNameRegex, is(''))
  }

  @Test void load_parameters_changes_script_regex_when_it_is_defined_in_parameters() {
    parameters['script regex'] = 'changed'
    job.scriptRegex = 'intact'
    job.loadParameters()
    assertThat(job.scriptRegex, is('changed'))
  }

  @Test void load_parameters_leaves_script_regex_intact_when_it_is_not_defined_in_parameters() {
    parameters['script regex'] = null
    job.scriptRegex = 'intact'
    job.loadParameters()
    assertThat(job.scriptRegex, is('intact'))
  }

  @Test void load_parameters_changes_script_regex_when_it_is_defined_in_parameters_and_is_empty() {
    parameters['script regex'] = ''
    job.scriptRegex = 'intact'
    job.loadParameters()
    assertThat(job.scriptRegex, is(''))
  }
}
