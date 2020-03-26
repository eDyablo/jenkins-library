import com.e4d.job.DataPreparationJob

def call(closure) {
  runPipelineJob(job: new DataPreparationJob(this),
    podTemplate: 'locust', closure)
}
