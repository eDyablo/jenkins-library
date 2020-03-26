import com.e4d.job.LoadTestingJob

def call(closure) {
  runPipelineJob(
    job: new LoadTestingJob(this), closure)
}
