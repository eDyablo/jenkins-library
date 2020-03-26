package com.e4d.job

import com.e4d.pipeline.DummyPipeline
import com.e4d.secret.SecretTextBearer
import java.net.URI
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class IntegrateHelmChartJobDeclarationTest {
  final pipeline = mock(DummyPipeline)
  final job = spy(new IntegrateHelmChartJob(pipeline))

  @Test void source_git_repository_sets_job_git_config_repository() {
    job.declare {
      source {
        gitRepository = 'git repository'
      }
    }
    assertThat(job.gitConfig.repository, is(equalTo('git repository')))
  }

  @Test void source_root_sets_job_source_root() {
    job.declare {
      source {
        root = 'source root'
      }
    }
    assertThat(job.sourceRoot, is(equalTo('source root')))
  }
}
