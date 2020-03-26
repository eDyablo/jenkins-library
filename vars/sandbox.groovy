import com.e4d.job.SandboxJob

def call(def closure) {
  runPipelineJob(new SandboxJob(this), closure)
}
