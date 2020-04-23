package com.e4d.job

import com.e4d.job.JobRunner
import com.e4d.pipeline.DummyPipeline
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class SetupHelmChartJobTest {
  final workflow = mock(DummyPipeline)
  final job = new SetupHelmChartJob(workflow)

  @Test void can_run_the_job_with_job_runner() {
    doReturn([:]).when(workflow).params
    doReturn([:]).when(workflow).env
    JobRunner.run(job)
  }
}
