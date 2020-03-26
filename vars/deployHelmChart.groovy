import com.e4d.job.DeployHelmChartJob
import com.e4d.ioc.ContextRegistry

def call(Closure closure) {
  ContextRegistry.registerDefaultContext(this)
  runPipelineJob(
    job: new DeployHelmChartJob(this),
    closure)
}
