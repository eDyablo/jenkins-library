import com.e4d.build.NameUtils
import com.e4d.build.PodTemplates
import com.e4d.job.PipelineJob

def call(Map options, Closure closure) {
  this.runPipelineJob(options, closure)
}

def call(PipelineJob job, Closure closure) {
  this.runPipelineJob(job: job, closure)
}

void runPipelineJob(Map options, Closure closure) {
  PipelineJob job = options.job
  job.declare(closure)
  def podLabel = options.podLabel ?: "jenkins-slave-${ NameUtils.shortHashedName(JOB_NAME) }-${BUILD_ID}".toLowerCase()
  def podTemplates = new PodTemplates()
  try
  {
    def jobParameters = job.defineParameters()
    if (jobParameters) {
      properties([ parameters(jobParameters) ])
    }
    job.loadParameters(params)
    job.initializeJob()
    final jobEnvVars = job.defineEnvVars()
    final jobVolumes = job.defineVolumes()
    final podOptions = [
      podLabel: podLabel,
      envVars: jobEnvVars,
      volumes: jobVolumes,
    ] + options
    podTemplates."${ options.podTemplate ?: 'default' }Template"(podOptions) {
      timeout(time: 60, unit: 'MINUTES') {
        node(podLabel) {
          job.run()
        }
      }
    }
    job.propagateSuccess()
  }
  catch (any) {
    job.propagateFailure()
    throw any
  }
}
