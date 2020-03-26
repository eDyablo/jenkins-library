# sandbox
Allows to run a script inside single predefined agent.
Bellow is a minimal script that prints greeting.
```groovy
sandbox {
  run {
    echo 'Hello, World!'
  }
}
```
You can use stages inside the run.
```groovy
sandbox {
  run {
    stage ('Greeting') {
      echo 'Hello, World!'
    }
  }
}
```
Also you can execute a shell script. For instance print out all environment variables defined for the agent.
```groovy
sandbox {
  run {
    stage ('Run in shell') {
      sh 'env'
    }
  }
}
```

# runPipelineJob
Runs an instance of [PipelineJob](/src/com/e4d/job/README.md#pipelinejob) using single agent created with predefined [Pod](https://kubernetes.io/docs/concepts/workloads/pods/pod-overview/) template.

It takes two parameters an instance of a pipeline job and a code that will be applied to the instance.

Let we have `GreetingJob` class that prints a greeting that is stored in its field named `greeting`.

_src/com/e4d/job/GreetingJob.groovy_
```groovy
package com.e4d.job

class GreetingJob extends PipelineJob {
  String geeting
  
  GreetingJob(pipeline) {
    super(pipeline)
  }
  
  def run() {
    pipeline.echo(greeting)
  }
}
```
_Jenkinsfile_
```groovy
import com.e4d.job.GreetingJob

runPipelineJob(new GreetingJob(this)) {
  greeting = 'Hello!'
}
```
Inside the code that you pass to `runPipelineJob` you can access any member field of `GreetingJob` and call any its member function. The code gets executed before call of `run` method of the job class.

# integrateService
Builds and tests micro-service from its source code.
## Example
```groovy
integrateService {
  generateJobParameters = false
  serviceConfig.service = 'myservice'
  gitConfig.baseUrl = 'https://github.com/organization'
  gitConfig.branch = 'develop'
  gitConfig.credsId = 'github-ci'
  pipConfig.configRef = 'secret : pip.config'
  nexusConfig.baseUrl = 'http://artifacts.com'
  nexusConfig.port = 8082
  nexusConfig.credsId = 'nexus.ci'
  nugetConfig.configRef = 'secret : nuget.config'
}
```
Option|Meaning|
-|-
**generateJobParameters** | If `true` adds options as parameters to the job and shows them in UI. Changes job's `Build Now` button to `Build with Parameters`
**serviceConfig** | Holds options specific for the service
serviceConfig.**service** | Name of the service (service's repository name)
**gitConfig** | Options specific for the git
gitConfig.**baseUrl** | URL to organization(user) hosts service's repository
gitConfig.**branch** | Name of the referenced branch
gitConfig.**credsId** | ID of credentials stored in Jekins
**pipConfig** | Options for Python's Pip
pipConfig.**configRef** | Reference to secret contains the configuration for Pip in form of column separated string `secret-name:secret-key`
**nexusConfig** | Holds Nexus specific options
nexusConfig.**baseUrl** | URL for Nexus service
nexusConfig.**port** | Port for Nexus service
nexusConfig.**credsId** | ID of credentials stored in Jenkins
**nugetConfig** | Options for nuget package manager
nugetConfig.**configRef** | Reference to secret contains the configuration for Nuget in form of column separated string `secret-name:secret-key`

# deployServiceImage
Deploys service that is alredy built.
## Example
```groovy
deployServiceImage {
  artifact = 'myservice:1.2.3'
  destination = 'cluster : namespace'
  nexusConfig.baseUrl = 'http://artifacts.com'
  nexusConfig.port = 8082
  nexusConfig.credsId = 'nexus.ci'
  k8sConfigRef = 'secret : kube.config'

  values = [
    service: [
      replicas: 1,
      limits: [
        cpu: '50m',
        memory: '150Mi'
      ],
      requests: [
        cpu: '50m',
        memory: '150Mi'
      ]
    ]
  ]
}
```
Option|Meaning|
-|-
**artifact** | Name of deployment artifact (result of [integrateService](#integrateService))
**destination** | Specifies k8s context and namespace(optional) to deploy into. Use column separated string like `context : namespace`. Context must be present in k8s configuration specified
**nexusConfig** | Holds Nexus specific options
nexusConfig.**baseUrl** | URL for Nexus service
nexusConfig.**port** | Port for Nexus service
nexusConfig.**credsId** | ID of credentials stored in Jenkins
**k8sConfigRef** | Specifies k8s configuration used as reference to secret contains the configuration in form of column separated string `secret-name : secret-key`
**values** | A map of enviroment specific values. Can contain any number of values composed from scalar values, lists and maps. The values can be referenced in k8s yaml files used for service's deployment
