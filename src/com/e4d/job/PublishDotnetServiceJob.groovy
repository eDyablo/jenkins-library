package com.e4d.job

import com.e4d.build.*
import com.e4d.config.*
import com.e4d.dotnet.*
import com.e4d.git.*
import com.e4d.pip.*
import com.e4d.k8s.*
import com.e4d.service.*

class PublishDotnetServiceJob extends PipelineJob {
  DotnetConfig dotnetConfig
  GitConfig gitConfig
  PipConfig pipConfig
  K8sConfig k8sConfig
  CSharpServiceConfig serviceConfig
  ConfigClient configClient
  DotnetClient dotnetClient
  K8sClient k8sClient
  ServiceReference service
  TemplateEngine templateEngine

  PublishDotnetServiceJob(pipeline) {
    super(pipeline)
    dotnetConfig = new DotnetConfig()
    gitConfig = new GitConfig()
    pipConfig = new PipConfig()
    k8sConfig = new K8sConfig()
    serviceConfig = new CSharpServiceConfig()
    configClient = new ConfigClient(pipeline, k8sConfig, pipConfig)
    dotnetClient = new DotnetClient(pipeline, dotnetConfig)
    k8sClient = new K8sClient(pipeline)
    templateEngine = new TemplateEngine(pipeline)
  }

  def defineParameters() {
    serviceConfig.defineParameters(pipeline) +
    dotnetConfig.defineParameters(pipeline) +
    gitConfig.defineParameters(pipeline) +
    pipConfig.defineParameters(pipeline) +
    k8sConfig.defineParameters(pipeline)
  }

  def loadParameters(params) {
    serviceConfig.loadParameters(params)
    dotnetConfig.loadParameters(params)
    gitConfig.loadParameters(params)
    pipConfig.loadParameters(params)
    k8sConfig.loadParameters(params)    
    service = serviceConfig.service
  }

  def defineEnvVars() {
    dotnetConfig.defineEnvVars(pipeline) +
    pipConfig.defineEnvVars(pipeline) +
    k8sConfig.defineEnvVars(pipeline)
  }

  def run() {
    def automationNamespace = "$service.name-$jobUID".toLowerCase()
    
    try {
      pipeline.stage('setup') {
        configClient.setup()
      }

      def source

      pipeline.stage('checkout') {
        source = pipeline.checkoutRecentSource(
            repository: service.sourceName,
            baseUrl: gitConfig.baseUrl,
            credsId: gitConfig.credsId,
            branch: gitConfig.branch)
      }

      pipeline.stage('configure') {
        def serviceConfigmap = "$service.name-config".toLowerCase()
        def serviceSecret = "$service.name-secret".toLowerCase()

        configClient.copySecrets(
          source: 'jenkins',
          destintion: automationNamespace,
          names: [
            'infra-secretconfigs',
            'infra-urls',
            'infra-credentials',
            'regsecret',
            serviceSecret])

        configClient.createConfigmaps(
          configDir: "$source.dir/configs",
          env: 'testing',
          prefix: service.name.toLowerCase(),
          namespace: automationNamespace,
          patch: templateEngine.render(serviceConfig.configmapPatch)
        )

        configClient.patchSecret(
          namespace: automationNamespace,
          name: serviceSecret,
          patch: templateEngine.render(serviceConfig.secretPatch)
        )
      }

      pipeline.stage('build') {
        dotnetClient.build(source.dir, serviceConfig.projectPattern,
            configuration: serviceConfig.buildConfiguration)
      }

      pipeline.stage('test') {
        def testResults = dotnetClient.test(source.dir,
            configuration: serviceConfig.buildConfiguration,
            includeProjects: serviceConfig.unitTestProjectPattern,
            testFilter: serviceConfig.testFilter)
        pipeline.publishTestReport(testResults)
      }
    }
    finally {
      pipeline.stage('clean') {
        pipeline.sh("""\
          #!/usr/bin/env bash
          kubectl delete namespace $automationNamespace
        """.stripIndent())
      }
    }
  }
}
