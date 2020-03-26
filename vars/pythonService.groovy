import com.e4d.job.IntegrateServiceJob

def call(Closure closure) {
  runPipelineJob(
    job: new IntegrateServiceJob(this),
    closure)
}
