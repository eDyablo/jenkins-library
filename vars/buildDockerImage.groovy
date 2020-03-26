import com.e4d.job.BuildDockerImageJob

def call(def closure) {
  runPipelineJob(new BuildDockerImageJob(this), closure)
}
