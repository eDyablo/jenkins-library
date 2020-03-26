package com.e4d.job

import hudson.model.ParametersDefinitionProperty
import hudson.model.StringParameterDefinition
import jenkins.model.Jenkins
import org.junit.*
import org.mockito.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class JobRunnerTest {
  final job = mock(Job)
  final runner = spy(new JobRunner(job))
  final jenkins = mock(Jenkins)

  @Before void beforeEachTest() {
    doReturn(jenkins).when(runner).jenkins
  }

  @Test void runs_its_job() {
    runner.run()
    verify(job).run()
  }

  @Test void loads_job_parameters_before_running_a_job() {
    runner.run()
    final order = inOrder(job)
    order.verify(job).loadParameters()
    order.verify(job).run()
  }

  @Test void initializes_job_before_running_it() {
    runner.run()
    final order = inOrder(job)
    order.verify(job).initialize()
    order.verify(job).run()
  }

  @Test void loads_parameters_before_initialization() {
    runner.run()
    final order = inOrder(job)
    order.verify(job).loadParameters()
    order.verify(job).initialize()
  }

  @Test void updates_job_parameters_after_run_a_job() {
    runner.run()
    final order = inOrder(job, runner)
    order.verify(job).run()
    order.verify(runner).updateJobParameters()
  }

  @Test void update_job_parameters_alters_existing_and_adds_extra_parameters() {
    doReturn('job full name').when(job).fullName
    final jobItem = mock(hudson.model.Job)
    doReturn(new ParametersDefinitionProperty(
      new StringParameterDefinition('first', 'intact'),
      new StringParameterDefinition('second', 'intact'),
    )).when(jobItem).getProperty(ParametersDefinitionProperty)
    doReturn(jobItem).when(jenkins).getItemByFullName('job full name', hudson.model.Job)

    doReturn([
      new StringParameterDefinition('second', 'altered'),
      new StringParameterDefinition('third', 'added'),
    ]).when(job).parameterDefinitions

    runner.updateJobParameters()

    final defintions = ArgumentCaptor.forClass(ParametersDefinitionProperty)
    final order = inOrder(jobItem)
    order.verify(jobItem).removeProperty(ParametersDefinitionProperty)
    order.verify(jobItem).addProperty(defintions.capture())
    order.verify(jobItem).save()
    assertThat(defintions.value.parameterDefinitions*.name, is(equalTo([
      'first', 'second', 'third'
    ])))
    assertThat(defintions.value.parameterDefinitions*.defaultValue, is(equalTo([
      'intact', 'altered', 'added'
    ])))
  }

  @Test void update_job_parameters_adds_extra_parameters_when_no_parameters_were_defined() {
    final jobItem = mock(hudson.model.Job)
    doReturn(null).when(jobItem).getProperty(ParametersDefinitionProperty)
    doReturn('name').when(job).fullName
    doReturn(jobItem).when(jenkins).getItemByFullName('name', hudson.model.Job)

    doReturn([
      new StringParameterDefinition('first', 'added'),
      new StringParameterDefinition('second', 'added'),
    ]).when(job).parameterDefinitions

    runner.updateJobParameters()

    final defintions = ArgumentCaptor.forClass(ParametersDefinitionProperty)
    verify(jobItem).addProperty(defintions.capture())
    assertThat(defintions.value.parameterDefinitions*.name,
      is(equalTo(['first', 'second'])))
    assertThat(defintions.value.parameterDefinitions*.defaultValue,
      is(equalTo(['added', 'added'])))
  }

  @Test void update_job_parameters_leaves_parameters_intact_when_no_new_parameters() {
    final jobItem = mock(hudson.model.Job)
    doReturn(new ParametersDefinitionProperty(
      new StringParameterDefinition('first', 'intact'),
      new StringParameterDefinition('second', 'intact'),
    )).when(jobItem).getProperty(ParametersDefinitionProperty)
    doReturn('name').when(job).fullName
    doReturn(jobItem).when(jenkins).getItemByFullName('name', hudson.model.Job)

    doReturn(null).when(job).parameterDefinitions

    runner.updateJobParameters()

    final defintions = ArgumentCaptor.forClass(ParametersDefinitionProperty)
    verify(jobItem).addProperty(defintions.capture())
    assertThat(defintions.value.parameterDefinitions*.name,
      is(equalTo(['first', 'second'])))
    assertThat(defintions.value.parameterDefinitions*.defaultValue,
      is(equalTo(['intact', 'intact'])))
  }

  @Test void update_job_parameters_does_nothing_when_no_parameters() {
    final jobItem = mock(hudson.model.Job)
    doReturn(null).when(jobItem).getProperty(ParametersDefinitionProperty)
    doReturn('name').when(job).fullName
    doReturn(jobItem).when(jenkins).getItemByFullName('name', hudson.model.Job)
    doReturn([]).when(job).parameterDefinitions

    runner.updateJobParameters()

    verify(jobItem, never()).removeProperty(ParametersDefinitionProperty)
    verify(jobItem, never()).addProperty(argThat(any(ParametersDefinitionProperty)))
    verify(jobItem, never()).save()
  }
}
