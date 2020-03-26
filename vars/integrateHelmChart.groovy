import com.e4d.job.IntegrateHelmChartJob
import com.e4d.ioc.ContextRegistry

def call(Closure closure) {
  ContextRegistry.registerDefaultContext(this)
  runPipelineJob(
    job: new IntegrateHelmChartJob(),
    closure)
}
