import com.e4d.job.IntegrateDockerImageJob
import com.e4d.ioc.ContextRegistry

def call(Closure declaration) {
  ContextRegistry.registerDefaultContext(this)
  runPipelineJob(
    job: new IntegrateDockerImageJob(this),
    declaration
  )
}
