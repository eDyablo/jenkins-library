package com.e4d.job

import com.e4d.git.GitSourceReference
import com.e4d.job.IntegrateNugetPackageJob
import hudson.model.StringParameterDefinition

class SetupNugetJob extends SetupJob {
  GitSourceReference sourceRef

  SetupNugetJob(workflow) {
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
    e4d.setupNuget {
    }
    '''
  }

  void run() {
    workflow.stage('setup') {
      final nugetJob = IntegrateNugetPackageJob.newInstance(workflow)
      nugetJob.gitSourceRef = sourceRef
      nugetJob.initializeJob()
      jenkins {
        folder('packages') {
          folder(nugetJob.gitSourceRef.repository) {
            folder('experiment') {
              job('itegrate') {
                script """
                  integrateNugetPackage {
                    source {
                      git '${ nugetJob.gitSourceRef.url }'
                    }
                    publish {
                      strategy {
                        skipPrereleaseVersion
                      }
                    }
                  }
                """
                discardBuilds inDays: 1
                gitHub {
                  project nugetJob.gitSourceRef.repositoryUrl
                  pullRequestBuilder {
                    organizations 'activehours', 'e4d'
                    updateCommitStatus context: 'integrate'
                  }
                }
              }
            }
            folder('develop') {
              job('integrate') {
                script """
                  integrateNugetPackage {
                    source {
                      git '${ nugetJob.gitSourceRef.url }'
                    }
                  }
                """
                discardBuilds afterAmount: 10
                gitHub {
                  project nugetJob.gitSourceRef.repositoryUrl
                  pushTrigger
                }
              }
            }
          }
        }
      }
    }
  }
}
