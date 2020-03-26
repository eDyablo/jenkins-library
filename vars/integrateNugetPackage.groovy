import com.e4d.job.IntegrateNugetPackageJob
import com.e4d.ioc.ContextRegistry

def call(Closure closure) {
  ContextRegistry.registerDefaultContext(this)
  runPipelineJob(
    job: new IntegrateNugetPackageJob(this),
    closure
  )
}
