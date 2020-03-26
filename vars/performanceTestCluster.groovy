import com.e4d.job.PerformanceTestClusterJob

def call(closure) {
  runPipelineJob(job: new PerformanceTestClusterJob(this), closure)
}
