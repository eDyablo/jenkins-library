import com.e4d.job.IntegrateServiceJob
import com.e4d.ioc.ContextRegistry

def call(Closure closure) {
  ContextRegistry.registerDefaultContext(this)
  runPipelineJob(
    job: new IntegrateServiceJob(),
    closure)
}
