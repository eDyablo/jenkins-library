import com.e4d.job.ServiceJob

def call(def closure) {
  def job = new ServiceJob(this)
  job.declare(closure)
  try
  {
    def jobParameters = job.defineParameters()
    if (!jobParameters.isEmpty()) {
      properties([ parameters(jobParameters) ])
    }
    job.loadParameters(params)
    job.run()
    job.propagateSuccess()
  }
  catch (any) {
    job.propagateFailure()
    throw any
  }
}
