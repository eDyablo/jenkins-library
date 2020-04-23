package com.e4d.job

class BootstrapJob extends SetupJob {
  BootstrapJob(workflow) {
    super(workflow)
  }

  void run() {
    workflow.stage('bootstrap') {
      jenkins {
        folder('lobby') {
          job('draft-git-repository-release') {
            displayName 'draft git repository release'
            final job = new DraftGitRepositoryReleaseJob(workflow)
            discardBuilds afterAmount: 20
            parameters job.parameterDefinitions
            script job.workflowScript
          }
          job('setup-docker-image') {
            displayName 'setup docker image'
            final job = new SetupDockerImageJob(workflow)
            discardBuilds afterAmount: 5
            parameters job.parameterDefinitions
            script job.workflowScript
          }
          job('setup-helm-chart') {
            displayName 'setup helm chart'
            final job = new SetupHelmChartJob(workflow)
            discardBuilds afterAmount: 5
            parameters job.parameterDefinitions
            script job.workflowScript
          }
          job('setup-nuget') {
            displayName 'setup nuget'
            final job = new SetupNugetJob(workflow)
            discardBuilds afterAmount: 5
            parameters job.parameterDefinitions
            script job.workflowScript
          }
          job('setup-service') {
            displayName 'setup service'
            final job = new SetupServiceJob(workflow)
            discardBuilds afterAmount: 5
            parameters job.parameterDefinitions
            script job.workflowScript
          }
        }
        folder('maintenance') {
          job('find-workflow-job') {
            displayName 'find workflow job'
            final job = new FindWorkflowJobJob(workflow)
            discardBuilds afterAmount: 10
            parameters job.parameterDefinitions
            script job.workflowScript
          }
          job('find-and-replace-script') {
            displayName 'find and replace script'
            final job = new FindAndReplaceScriptJob(workflow)
            discardBuilds afterAmount: 10
            parameters job.parameterDefinitions
            script job.workflowScript
            description '''
              <h1><font color='red'>Use it with causion!<font color='red'></h1>
              <h2>It might break or delete job scripts</h2>
              <p>There are a <b>'dry run'</b> option which is set by default.</p>
            '''
          }
        }
      }
    }
  }
}
