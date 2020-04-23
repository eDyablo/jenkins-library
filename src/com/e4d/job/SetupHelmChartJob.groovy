package com.e4d.job

import com.e4d.git.GitSourceReference
import com.e4d.job.IntegrateHelmChartJob
import hudson.model.StringParameterDefinition

class SetupHelmChartJob extends SetupJob {
  GitSourceReference sourceRef
  String imageName = ''

  SetupHelmChartJob(workflow) {
    super(workflow)
  }

  def getParameterDefinitions() {
    [
      new StringParameterDefinition(
        'source git', sourceRef?.toString(), '', true
      ),
    ]
  }

  void loadParameters() {
    sourceRef = workflow.params['source git'] ? new GitSourceReference(workflow.params['source git']) : sourceRef
  }

  String getWorkflowScript() {
    '''
    e4d.setupHelmChart {
    }
    '''
  }

  void run() {
    workflow.stage('setup') {
      final helmJob = IntegrateHelmChartJob.newInstance(workflow)
      helmJob.gitSourceRef = sourceRef
      helmJob.initializeJob()
      jenkins {
        folder('apps') {
          folder(helmJob.gitSourceRef.repository) {
            folder('experiment') {
              job('integrate') {
                script generateIntegrateJobScript(
                  sourceGit: helmJob.gitSourceRef.url,
                )
                discardBuilds inDays: 1
                gitHub {
                  project helmJob.gitSourceRef.repositoryUrl
                  pullRequestBuilder {
                    organizations 'activehours', 'e4d'
                    updateCommitStatus context: 'integrate'
                  }
                }
              }
              job('deploy') {
                script generateDeployJobScript(
                  destination: 'dev : e4d-ci',
                )
                discardBuilds inDays: 1
              }
            }
            folder('develop') {
              job('integrate') {
                script generateIntegrateJobScript(
                  sourceGit: helmJob.gitSourceRef.url,
                )
                discardBuilds afterAmount: 10
                gitHub {
                  project helmJob.gitSourceRef.repositoryUrl
                  pushTrigger
                }
              }
              job('deploy') {
                script generateDeployJobScript(
                  destination: 'dev',
                )
                discardBuilds afterAmount: 10
              }
            }
            folder('production') {
              job('integrate') {
                script generateIntegrateJobScript(
                  sourceGit: helmJob.gitSourceRef.url,
                )
                discardBuilds afterAmount: 15
              }
              job('deploy') {
                script generateDeployJobScript(
                  destination: 'production',
                )
                discardBuilds afterAmount: 15
              }
            }
          }
        }
      }
    }
  }

  String generateIntegrateJobScript(Map options=[:]) {
    final script = new StringBuilder()
    script << '''\
      |integrateHelmChart {
    '''
    if (options.sourceGit) {
      script << """\
        |  source {
        |    git '${ options.sourceGit }'
        |  }
      """
    }
    script << '''\
      |}
    '''
    script.toString().stripMargin()
  }

  String generateDeployJobScript(Map options=[:]) {
    final script = new StringBuilder()
    script << '''\
      |deployHelmChart {
    '''
    if (options.destination) {
      script << """\
        |  destination '${ options.destination }'
      """
    }
    script << '''\
      |}
    '''
    script.toString().stripMargin()
  }
}
