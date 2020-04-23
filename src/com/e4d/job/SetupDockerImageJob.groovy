package com.e4d.job

import com.e4d.git.GitSourceReference
import com.e4d.job.IntegrateDockerImageJob
import hudson.model.StringParameterDefinition

class SetupDockerImageJob extends SetupJob {
  GitSourceReference sourceRef
  String imageName = ''

  SetupDockerImageJob(workflow) {
    super(workflow)
  }

  def getParameterDefinitions() {
    [
      new StringParameterDefinition(
        'source git', sourceRef?.toString(), '', true
      ),
      new StringParameterDefinition(
        'image name', imageName, '', true
      ),
    ]
  }

  void loadParameters() {
    sourceRef = workflow.params['source git'] ? new GitSourceReference(workflow.params['source git']) : sourceRef
    imageName = workflow.params['image name'] ?: imageName
  }

  String getWorkflowScript() {
    '''
    e4d.setupDockerImage {
    }
    '''
  }

  void run() {
    workflow.stage('setup') {
      final imageJob = IntegrateDockerImageJob.newInstance(workflow)
      imageJob.gitSourceRef = sourceRef
      imageJob.initializeJob()
      jenkins {
        folder('images') {
          folder(imageName ?: imageJob.gitSourceRef.repository) {
            folder('experiment') {
              job('itegrate') {
                script generateIntegrateJobScript(
                  sourceGit: imageJob.gitSourceRef.url,
                  artifactBaseName: imageName,
                  skipPrereleaseVersion: true,
                )
                discardBuilds inDays: 1
                gitHub {
                  project imageJob.gitSourceRef.repositoryUrl
                  pullRequestBuilder {
                    organizations 'activehours', 'e4d'
                    updateCommitStatus context: 'integrate'
                  }
                }
              }
            }
            folder('develop') {
              job('integrate') {
                script generateIntegrateJobScript(
                  sourceGit: imageJob.gitSourceRef.url,
                  artifactBaseName: imageName,
                )
                discardBuilds afterAmount: 10
                gitHub {
                  project imageJob.gitSourceRef.repositoryUrl
                  pushTrigger
                }
              }
            }
          }
        }
      }
    }
  }

  String generateIntegrateJobScript(Map options) {
    final script = new StringBuilder()
    script << '''\
      |integrateDockerImage {
    '''
    if (options.sourceGit) {
      script << """\
        |  source {
        |    git '${ options.sourceGit }'
        |  }
      """
    }
    if (options.artifactBaseName) {
      script << """\
        |  artifact {
        |    baseName '${ options.artifactBaseName }'
        |  }
      """
    }
    if (options.skipPrereleaseVersion) {
      script << '''\
        |  publish {
        |    stategy {
        |      skipPrereleaseVersion  
        |    }
        |  }
      '''
    }
    script << '''\
      |}
    '''
    script.toString().stripMargin()
  }
}
