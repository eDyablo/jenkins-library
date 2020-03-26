import com.e4d.job.HttpClientGeneratorJob
import com.e4d.ioc.ContextRegistry
import com.e4d.pipeline.PipelineShell

def call(Closure closure) {
  ContextRegistry.registerDefaultContext(this)
  runPipelineJob(
    job: new HttpClientGeneratorJob(this),
    closure
  )
}
