package com.e4d.job

import com.e4d.pipeline.DummyPipeline
import org.junit.*
import org.mockito.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*

class LoadTestingJobTest {
  final pipeline = spy(DummyPipeline)
  final job = spy(new LoadTestingJob(pipeline))

  @Test void does_warmup_step_with_half_of_specified_concurrency() {
    [
      [1, 1],
      [2, 1],
      [3, 1],
      [4, 2],
      [5, 2],
    ].each { concurrency, warmupConcurrency ->
      reset(job)
      doReturn('{}').when(job).runSlowCooker(any(), any(), any(), any(), any(), any())
      job.concurrency = concurrency
      job.run()
      verify(job, atLeast(1)).runSlowCooker(any(), eq(warmupConcurrency), any(), any(), any(), any())
    }
  }

  @Test void does_warmup_step_with_half_of_specified_requests_when_number_of_requests_is_less_than_or_equal_to_2000() {
    [
      [   1, 1],
      [   2, 1],
      [   3, 1],
      [  10, 5],
      [  15, 7],
      [ 100, 50],
      [1000, 500],
      [1999, 999],
      [2000, 1000],
      [2001, 1000],
    ].each { total, warmupTotal ->
      reset(job)
      doReturn('{}').when(job).runSlowCooker(any(), any(), any(), any(), any(), any())
      job.totalRequests = total
      job.run()
      verify(job, atLeast(1)).runSlowCooker(any(), any(), any(), eq(warmupTotal), any(), any())
    }
  }
} 
