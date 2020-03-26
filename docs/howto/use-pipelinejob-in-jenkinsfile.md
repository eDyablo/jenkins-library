# Use pipeline job
When you have [defined your job](./define-new-job.md) you can write the following Jenkinsfile.
```groovy
import com.e4d.job.SampleJob

runPipelineJob(new SampleJob(this)) {
}
```
The script uses [`runPipelineJob`](/vars/README.md#runpipelinejob) keyword. But you can define a keyword for the job so the Jenkinsfile will look like below.
```groovy
sample {
}
```
To define your keyword.
1. Inside [vars](/vars) directory create file `sample.groovy`. The name of the file must be equal to your keyword.
2. Put the following code into the file.
```groovy
import com.e4d.job.SampleJob

def call(Closure closure) {
  runPipelineJob(new SampleJob(this), closure)
}
```
### See also [how to](/README.md#howto)
- [Define a new job](/docs/howto/define-new-job.md)
