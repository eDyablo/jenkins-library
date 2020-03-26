import com.e4d.job.DeployServiceJob
import com.e4d.ioc.ContextRegistry

def call(Closure closure) {
  ContextRegistry.registerDefaultContext(this)
  runPipelineJob(
    job: new DeployServiceJob(),
    closure)
}
