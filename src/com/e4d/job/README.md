# SandboxJob
Allows to execute a [groovy](http://groovy-lang.org/) script.

# PipelineJob
A base class for all jobs that use [Pipeline plug-in](https://wiki.jenkins.io/display/JENKINS/Pipeline+Plugin).

To create your own job you need to create class that extends the PipelineJob and overrides its `run` method.
```groovy
class GreetingJob extends PipelineJob {
  String geeting = 'Hello, world!'
  
  GreetingJob(pipeline) {
    super(pipeline)
  }
  
  def run() {
    pipeline.echo(greeting)
  }
}
```
## Parametrized pipeline job
You can define job parameters that can be used to fire the job from another job via [pipeline's `build` function](https://jenkins.io/doc/pipeline/steps/pipeline-build-step). The job's paramters will be shown in Jenkins's UI when you hit [`Build with Parameters`](https://wiki.jenkins.io/display/JENKINS/Parameterized+Build). You have to override `defineParameters` and `loadParameters` methods.
```groovy
class GreetingJob extends PipelineJob {
  String geeting = 'Hello, world!'
  
  GreetingJob(pipeline) {
    super(pipeline)
  }
  
  def defineParameters() {
    [ pipeline.string(name: 'GREETING', defaultValue: greeting, description: 'Text of greeting') ]
  }
  
  def loadParameters(params) {
    greeting = params.GREETING
  }
  
  def run() {
    pipeline.echo(greeting)
  }
}
```

The `defineParameters` must return list of [parameter definitions](https://jenkins.io/doc/book/pipeline/syntax/#parameters). You can use field members of any type as parameters but you have to provide convertions.
```groovy
class SampleJob extends PipelineJob {
  String name
  int age
  
  SampleJob(pipeline) {
    super(pipeline)
  }
  
  def defineParameters() {
    [ pipeline.string(name: 'NAME', defaultValue: name),
      pipeline.string(name: 'AGE', defaultValue: "$age") ]
  }
  
  def loadParameters(params) {
    name = params.NAME
    age = Integer.parseInt(params.AGE)
  }
  
  def run() {
    pipeline.echo("$name $age")
  }
}
```
## Pod's evironment variables
Similarly you can provide environment variables for an agent that your job will run in. You have to override `defineEnvVars` method.
```groovy
class SampleJob extends PipelineJob {
  String name
  int age
  
  SampleJob(pipeline) {
    super(pipleine)
  }
  
  def defineEnvVars() {
    [ pipeline.envVar(key: 'NAME', value: name),
      pipeline.envVar(key: 'AGE', value: "$age") ]
  }
  
  def run() {
    pipeline.sh('echo $NAME $AGE')
  }
}
```
If you need to define environment variable with sensitive data stored in secrets you can use `secretEnvVar` function of [Kubernetes plug-in](https://wiki.jenkins.io/display/JENKINS/Kubernetes+Plugin).
```groovy
def defineEnvVars() {
  [ pipeline.secretEnvVar(key: 'PASSWORD', secretName: 'secret', secretKey: 'password') ]
}
```
## Combine and re-use parameters
Often you have several parameters that define one logical element of your configuration. For instance you might need url and credentials that gives you an access to a service. The url and credentials are coupled and cannot be used separately. It is recomended to put them into distinct module.
```groovy
class ServiceConfig {
  String hostUrl
  String credsId
  
  def defineParameters(pipeline) {
    [ pipeline.string(name: 'SERVICE_HOST_URL', defaultValue: hostUrl),
      pipeline.string(name: 'SERVICE_CREDS_ID', defaultValue: credsId) ]
  }
  
  def loadParameters(params) {
    hostUrl = params.SERVICE_HOST_URL
    credsId = params.SERVICE_CREDS_ID
  }
}
```
Then any job can use the pre-defined parameters.
```groovy
class ServiceJob extends PipelineJob {
  ServiceConfig service
  
  SqlJob(pipeline) {
    super(pipeline)
    service = new ServiceConfig()
  }
  
  def defineParameters() {
    service.defineParameters(pipeline)
  }
  
  def loadParameters(params) {
    service.loadParameters(params)
  }
}
```
We have several pre-defined parameters for tools that we use our in pipeline jobs.
- [git](/src/com/e4d/git/GitConfig.groovy)
- [kubernetes](/src/com/e4d/k8s/K8sConfig.groovy)
- [mysql](/src/com/e4d/mysql/MySqlConfig.groovy)
- [nexus](/src/com/e4d/nexus/NexusConfig.groovy)
### Use multiple configurations in one job
When your job requires more than one pre-defined configuration you can use them as shown below.
```groovy
class SampleJob extends PipelineJob {
  GitConfig git
  K8sConfig k8s
  NexusConfig nexus
  
  SampleJob(pipeline) {
    super(pipeline)
    git = new GitConfig(credsId: pipeline.GLOBAL_GITSCM_CREDSID)
    k8s = new K8sConfig()
    nexus = new NexusConfig()
  }
  
  def defineParameters() {
    git.defineParameters(pipeline)
      .plus(k8s.defineParameters(pipeline))
      .plus(nexus.defineParameters(pipeline))
  }

  def loadParameters(params) {
    git.loadParameters(params)
    k8s.loadParameters(params)
    nexus.loadParameters(params)
  }
}
```
The order the configurations appear in `defineParameters` determines the order they appear in Jenkins's UI.
