package com.e4d.job

import com.e4d.git.GitConfig
import com.e4d.git.GitSourceReference
import com.e4d.job.IntegrateNugetPackageJob
import hudson.model.StringParameterDefinition

class SetupServiceJob extends SetupJob {
  GitSourceReference sourceGit
  String serviceName

  SetupServiceJob(workflow) {
    super(workflow)
  }

  def getParameterDefinitions() {
    [
      new StringParameterDefinition(
        'service name', serviceName, '', true
      ),
      new StringParameterDefinition(
        'source git', sourceGit?.toString(), '', true
      ),
    ]
  }

  void loadParameters() {
    serviceName = workflow.params['service name'] ?: serviceName
    sourceGit = workflow.params['source git'] ? new GitSourceReference(workflow.params['source git']) : sourceGit
  }

  void initialize() {
    sourceGit = sourceGit ?: new GitSourceReference(serviceName)
    serviceName = serviceName ?: sourceGit.repository
  }

  GitSourceReference getResolvedSourceRef() {
    final gitConfig = new GitConfig()
    gitConfig.with(DefaultValues.git)
    new GitSourceReference(
      scheme: sourceGit.host
        ? sourceGit.scheme
        : (gitConfig.host ? gitConfig.protocol : sourceGit.scheme),
      host: sourceGit.host ?: gitConfig.host,
      owner: sourceGit.owner ?: gitConfig.owner,
      repository: sourceGit.repository,
      branch: sourceGit.branch ?: gitConfig.branch,
    )
  }

  String getWorkflowScript() {
    '''
    e4d.setupService {
    }
    '''
  }

  void run() {
    workflow.stage('setup') {
      final sourceRef = resolvedSourceRef
      final integrationScript = generateIntegrateJobScript()
      jenkins {
        folder('apps') {
          folder(serviceName) {
            folder('experiment') {
              job('integrate') {
                script integrationScript
                discardBuilds inDays: 1
                gitHub {
                  project sourceRef.repositoryUrl
                  pullRequestBuilder {
                    organizations 'activehours', 'e4d'
                    updateCommitStatus context: 'integrate'
                  }
                }
              }
              job('deploy') {
                script """
                  deployServiceImage {
                    destination = 'dev : e4d-ci'

                    rollback {
                      always
                    }
                  }
                """
                discardBuilds inDays: 1
                disableConcurrentBuilds
              }
            }
            folder('develop') {
              job('integrate') {
                script integrationScript
                discardBuilds afterAmount: 5
                gitHub {
                  project sourceRef.repositoryUrl
                  pushTrigger
                }
              }
              job('deploy') {
                script """
                  deployServiceImage {
                    destination = 'dev'
                    values = [
                      deployment: [
                        environment: 'dev'
                      ],
                    ]
                  }
                """
                discardBuilds afterAmount: 5
                disableConcurrentBuilds
              }
            }
            folder('production') {
              job('integrate') {
                script integrationScript
                discardBuilds afterAmount: 10
                gitHub {
                  project sourceRef.repositoryUrl
                }
              }
              job('deploy') {
                script """
                  deployServiceImage {
                    destination = 'production'

                    options.testing.skip = true

                    values = [
                      deployment: [
                        environment: 'production'
                      ],
                    ]

                    notify {
                      success {
                        leadTimeTracker service: '${ serviceName }'
                      }
                      failure {
                        slack url: '#e4d.slack.global'
                      }
                    }
                  }
                """
                discardBuilds afterAmount: 10
                disableConcurrentBuilds
              }
            }
          }
        }
      }
    }
  }

  String generateIntegrateJobScript() {
    final script = ['|integrateService {']

    script << "|  serviceConfig.service = '${ serviceName }'"

    if (sourceGit.organizationUrl) {
      script << "|  gitConfig.baseUrl = '${ sourceGit.organizationUrl }'"
    } else if (sourceGit.organization) {
      script << "|  gitConfig.owner = '${ sourceGit.organization }'"
    }

    if (sourceGit.repository && sourceGit.repository != serviceName) {
      script << "|  gitConfig.repository = '${ sourceGit.repository }'"
    }

    if (sourceGit.branch) {
      script << "|  gitConfig.branch = '${ sourceGit.branch }'"
    }

    if (sourceGit.directory) {
      script << "|  sourceRootDir ='${ sourceGit.directory }'"
    }

    script << '|  deploymentJob = \'deploy\''

    script << '|}'

    script.join('\n').stripMargin()
  }
}
