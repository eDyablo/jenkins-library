import com.e4d.job.CleanUpDbJob

def call(def closure) {
  runPipelineJob(new CleanUpDbJob(this), closure)
}
