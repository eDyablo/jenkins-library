import com.e4d.job.TestServiceJob

def call(Map options=[:], Closure closure) {
  runPipelineJob(
    job: new TestServiceJob(options, this),
    closure)
}

def call(Map options=[:], String service, Closure closure) {
  call(options + [service: service], closure)
}
