import com.e4d.job.PerformanceTestJob

def call(closure) {
    runPipelineJob(job: new PerformanceTestJob(this),
      podTemplate: 'locust', closure)
}
