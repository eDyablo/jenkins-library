import com.e4d.ioc.ContextRegistry
import com.e4d.job.IntegratePythonPackageJob
import com.e4d.pipeline.PipelineShell

def call(Closure declaration) {
  ContextRegistry.registerDefaultContext(this)
  runPipelineJob(
    job: new IntegratePythonPackageJob(this, new PipelineShell(this)),
    declaration)
}

def call(String path, Closure declaration={}) {
  ContextRegistry.registerDefaultContext(this)
  final def job = new IntegratePythonPackageJob(this, new PipelineShell(this))
  def (repository, sourceRoot) = extractRepositoryAndPath(path)
  job.gitConfig.repository = repository
  job.sourceRoot = sourceRoot
  runPipelineJob(job: job, declaration)
}

def extractRepositoryAndPath(String text) {
  final def tokens = text?.tokenize('/') ?: []
  return [
    tokens.take(1).join(''),
    tokens.drop(1).join('/'),
  ]
}
