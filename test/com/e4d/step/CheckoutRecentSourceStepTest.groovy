package com.e4d.step

import com.e4d.ioc.Context
import com.e4d.ioc.ContextRegistry
import com.e4d.pipeline.DummyPipeline

import org.junit.*
import org.mockito.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any
import static com.e4d.mockito.Matchers.*

class CheckoutRecentSourceStepTest {
  final def context = mock(Context.class)
  final def pipeline = mock(DummyPipeline.class, CALLS_REAL_METHODS)

  @Before void beforeEachTest() {
    when(context.pipeline).thenReturn(pipeline)
    when(pipeline.checkout(any(Map.class))).thenReturn([:])
    when(pipeline.sh(any(Map.class))).thenReturn('{}')
    ContextRegistry.registerContext(context)
  }

  @Test void uses_base_url_and_repository_specified_in_constuctor() {
    new CheckoutRecentSourceStep(repository: 'repository', baseUrl: 'base url').run()

    final def otpions = ArgumentCaptor.forClass(Map.class)
    verify(pipeline).checkout(otpions.capture())
    assertThat(otpions.value.userRemoteConfigs.first().url.toString(),
      is('base url/repository'))
  }

  @Test void uses_git_base_url_from_params_when_base_url_is_not_specified_in_constuctor() {
    when(pipeline.params).thenReturn([GIT_BASE_URL: 'git base url'])
  
    new CheckoutRecentSourceStep(repository: 'repository').run()

    final def otpions = ArgumentCaptor.forClass(Map.class)
    verify(pipeline).checkout(otpions.capture())
    assertThat(otpions.value.userRemoteConfigs.first().url.toString(),
      is('git base url/repository'))
  }

  @Test void uses_branch_specified_in_constuctor() {
    new CheckoutRecentSourceStep(branch: 'branch').run()

    final def otpions = ArgumentCaptor.forClass(Map.class)
    verify(pipeline).checkout(otpions.capture())
    assertThat(otpions.value.branches.first().name, is('branch'))
  }

  @Test void uses_git_branch_from_params_when_branch_is_not_specified_in_constuctor() {
    when(pipeline.params).thenReturn([GIT_BRANCH: 'git branch'])
  
    new CheckoutRecentSourceStep([:]).run()

    final def otpions = ArgumentCaptor.forClass(Map.class)
    verify(pipeline).checkout(otpions.capture())
    assertThat(otpions.value.branches.first().name, is('git branch'))
  }

  @Test void uses_creds_id_specified_in_constuctor() {
    new CheckoutRecentSourceStep(credsId: 'creds id').run()

    final def otpions = ArgumentCaptor.forClass(Map.class)
    verify(pipeline).checkout(otpions.capture())
    assertThat(otpions.value.userRemoteConfigs.first().credentialsId, is('creds id'))
  }

  @Test void uses_git_creds_id_from_params_when_no_creds_id_specified_in_constuctor() {
    when(pipeline.params).thenReturn([GIT_CREDS_ID: 'git creds id'])

    new CheckoutRecentSourceStep([:]).run()

    final def otpions = ArgumentCaptor.forClass(Map.class)
    verify(pipeline).checkout(otpions.capture())
    assertThat(otpions.value.userRemoteConfigs.first().credentialsId, is('git creds id'))
  }

  @Test void run_returns_changed_files_returned_by_git_describe_checkout_script() {
    when(pipeline.sh(mapContains(script: 'com/e4d/git/git-describe-checkout.sh')))
      .thenReturn('''
      {
        "diff": {
          "files": [
            "file 1",
            "file 2"
          ]
        }
      }''')
    final def result = new CheckoutRecentSourceStep([:]).run()
    assertThat(result.changedFiles, is(['file 1', 'file 2']))
  }

  @Test void run_returns_no_changed_files_when_no_files_returned_by_git_describe_checkout_script() {
    when(pipeline.sh(mapContains(script: 'com/e4d/git/git-describe-checkout.sh')))
      .thenReturn('''
      {
        "diff": {
          "files": [
          ]
        }
      }''')
    final def result = new CheckoutRecentSourceStep([:]).run()
    assertThat(result.changedFiles, is([]))
  }

  @Test void run_returns_no_changed_files_when_git_describe_checkout_script_returns_empty_json() {
    when(pipeline.sh(mapContains(script: 'com/e4d/git/git-describe-checkout.sh')))
      .thenReturn('{}')
    final def result = new CheckoutRecentSourceStep([:]).run()
    assertThat(result.changedFiles, is([]))
  }

  @Test void run_returns_result_contains_timestamp_from_json_returned_by_git_describe_checkout_script() {
    when(pipeline.sh(mapContains(script: 'com/e4d/git/git-describe-checkout.sh')))
      .thenReturn('{"timestamp":1234567890}')
    final def result = new CheckoutRecentSourceStep([:]).run()
    assertThat(result.timestamp, is(1234567890))
  }

  @Test void run_returns_result_contains_revison_from_json_returned_by_git_describe_checkout_script() {
    when(pipeline.sh(mapContains(script: 'com/e4d/git/git-describe-checkout.sh')))
      .thenReturn('{"revision":123}')
    final def result = new CheckoutRecentSourceStep([:]).run()
    assertThat(result.revision, is(123))
  }

  @Test void run_returns_result_contains_hash_from_json_returned_by_git_describe_checkout_script() {
    when(pipeline.sh(mapContains(script: 'com/e4d/git/git-describe-checkout.sh')))
      .thenReturn('{"hash":"deadbeaf"}')
    final def result = new CheckoutRecentSourceStep([:]).run()
    assertThat(result.hash, is("deadbeaf"))
  }
}
