package com.e4d.job

import com.e4d.job.Option
import com.e4d.pipeline.DummyPipeline
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class DeployServiceJobDeclarationTest {
  final pipeline = spy(DummyPipeline)
  final job = spy(new DeployServiceJob(pipeline))

  @Test void rollback_always_sets_proper_job_option() {
    // Act
    job.declare {
      rollback {
        always
      }
    }
    // Assert
    assertThat(job.options.rollback.when, is(equalTo(Option.When.ALWAYS)))
  }

  @Test void rollback_on_failure_sets_proper_job_option() {
    // Act
    job.declare {
      rollback {
        onFailure
      }
    }
    // Assert
    assertThat(job.options.rollback.when, is(equalTo(Option.When.ON_FAILURE)))
  }
}
