# Define a new job

1. Inside [src/com/e4d/job](/src/com/e4d/job) create a file `SampleJob.groovy`. Where `SampleJob` is the name of your job. We recommend to postfix the file name with `Job`.
2. Put there the following code
```groovy
package com.e4d.job

class SampleJob extends PipelineJob {
  SampleJob(pipeline) {
    super(pipeline)
  }

  def run() {
  }
}
```
3. Extend code of the job according to the [guide](/src/com/e4d/job/README.md#pipelinejob).
### See also [how to](/README.md#howto)
- [Use pipeline job](/docs/howto/use-pipelinejob-in-jenkinsfile.md)
- [Use sandbox job](/docs/howto/use-sandbox-job.md)
