package com.e4d.step

import com.e4d.ioc.Context
import com.e4d.ioc.ContextRegistry
import com.e4d.pipeline.DummyPipeline

import org.junit.*
import org.mockito.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*

class CheckoutRecentSourceStepInternalsTest {
  final def context = mock(Context.class)
  final def pipeline = mock(DummyPipeline.class, CALLS_REAL_METHODS)
  CheckoutRecentSourceStep step

  @Before void beforeEachTest() {
    when(context.pipeline).thenReturn(pipeline)
    ContextRegistry.registerContext(context)
    step = new CheckoutRecentSourceStep([:])
  }

  @Test void extractRemote_returns_null_when_branch_is_null() {
    assertThat(step.extractRemote(), is(null))
  }

  @Test void extractRemote_returns_empty_string_when_branch_is_empty() {
    assertThat(step.extractRemote(''), is(''))
  }

  @Test void extractRemote_returns_origin_when_branch_contains_no_slash() {
    assertThat(step.extractRemote('branch'), is('origin'))
  }

  @Test void extractRemote_returns_string_contains_all_symbols_before_first_slash() {
    assertThat(step.extractRemote('first/second/third'), is('first'))
  }

  @Test void findTargetRevision_returns_git_previous_successful_commit_from_checkout_when_no_pull_request_target_branch() {
    when(pipeline.params).thenReturn([ghprbTargetBranch: null])
    final def checkout = [
      GIT_PREVIOUS_SUCCESSFUL_COMMIT: 'git previous successfull commit'
    ]
    assertThat(step.findTargetRevision(checkout),
      is('git previous successfull commit'))
  }
  
  @Test void findTargetRevision_returns_pull_request_target_branch_when_it_is_present_in_params() {
    when(pipeline.params).thenReturn([ghprbTargetBranch: 'target branch'])
    assertThat(step.findTargetRevision([:]), is('target branch'))
  }

  @Test void findTargetRevision_returns_empty_string_when_no_pull_request_target_branch_and_no_successful_previous_commit() {
    when(pipeline.params).thenReturn([ghprbTargetBranch: null])
    final def checkout = [GIT_PREVIOUS_SUCCESSFUL_COMMIT: null]
    assertThat(step.findTargetRevision(checkout), is(emptyString()))
  }
}
