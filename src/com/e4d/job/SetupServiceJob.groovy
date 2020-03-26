package com.e4d.job

import com.cloudbees.hudson.plugins.folder.Folder
import com.e4d.git.GitSourceReference
import hudson.model.Items
import hudson.model.ParametersDefinitionProperty
import hudson.model.StringParameterDefinition
import hudson.tasks.LogRotator
import jenkins.model.BuildDiscarderProperty
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

class SetupServiceJob implements Job {
  final def workflow
  GitSourceReference sourceGit
  String serviceName

  SetupServiceJob(workflow) {
    this.workflow = workflow
  }

  Jenkins getJenkins() {
    Jenkins.get()
  }

  String getFullName() {
    workflow.env.JOB_NAME
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

  void run() {
    workflow.stage('setup') {
      setupService()
    }
  }

  void setupService() {
    final applications = jenkins.allItems(Folder).find {
      it.fullName == 'zapps'
    } as Folder ?: jenkins.createProject(Folder, 'zapps') as Folder

    final service = applications.allItems(Folder).find {
      it.name == serviceName
    } as Folder ?: applications.createProject(Folder, serviceName) as Folder

    [
      [name: 'experiment', destination: 'dev : e4d-ci'],
      [name: 'develop', destination: 'dev'],
      [name: 'production', destination: 'production'],
    ].each { options ->
      setupEnv(options, service)
    }
  }

  void setupEnv(Map options=[:], Folder service) {
    final env = service.allItems(Folder).find {
      it.name == options.name
    } as Folder ?: service.createProject(Folder, options.name) as Folder

    setupIntegrate(options, env)
    setupDeploy(options, env)
  }

  void setupIntegrate(Map options=[:], Folder env) {
    final integrate = env.allItems(WorkflowJob).find {
      it.name == 'integrate'
    } as WorkflowJob ?: env.createProject(WorkflowJob, 'integrate') as WorkflowJob

    integrate.definition = new CpsFlowDefinition(
      integrateServiceScript, true)

    integrate.addProperty(new BuildDiscarderProperty(
      new LogRotator('1', '', '', '')))

    integrate.save()
  }

  String getIntegrateServiceScript() {
    final script = ['integrateService {']

    script << "\tserviceConfig.service = '${ serviceName }'"

    if (sourceGit.organizationUrl) {
      script << "\tgitConfig.baseUrl = '${ sourceGit.organizationUrl }'"
    } else if (sourceGit.organization) {
      script << "\tgitConfig.owner = '${ sourceGit.organization }'"
    }

    if (sourceGit.repository && sourceGit.repository != serviceName) {
      script << "\tgitConfig.repository = '${ sourceGit.repository }'"
    }

    if (sourceGit.branch) {
      script << "\tgitConfig.branch = '${ sourceGit.branch }'"
    }

    if (sourceGit.directory) {
      script << "\tsourceRootDir ='${ sourceGit.directory }'"
    }

    script << '\tdeploymentJob = \'deploy\''

    script << '}'

    script.join('\n')
  }

  void setupDeploy(Map options=[:], Folder env) {
    final deploy = env.allItems(WorkflowJob).find {
      it.name == 'deploy'
    } as WorkflowJob ?: env.createProject(WorkflowJob, 'deploy') as WorkflowJob

    deploy.definition = new CpsFlowDefinition("""\
      deployServiceImage {
        destination = '${ options.destination }'
      }
    """.stripIndent(), true)

    deploy.addProperty(new BuildDiscarderProperty(
      new LogRotator('1', '', '', '')))

    deploy.save()
  }
}
