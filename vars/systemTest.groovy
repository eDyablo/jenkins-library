import com.e4d.job.SystemTestJob

def call(closure) {
    runPipelineJob(job: new SystemTestJob(this),
        podTemplate: 'systemtest', closure)
}

