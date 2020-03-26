package com.e4d.job

import com.e4d.build.ArtifactReference
import com.e4d.docker.*
import com.e4d.dotnet.*
import com.e4d.nexus.*
import com.e4d.k8s.*
import com.e4d.pipeline.*
import com.e4d.service.ServiceInstanceReference
import com.cloudbees.groovy.cps.NonCPS

class TestServiceJob extends PipelineJob {
  ArtifactReference artifact
  final K8sClient k8s
  final K8sConfig k8sConfig = new K8sConfig()
  final NexusConfig nexusConfig = new NexusConfig()
  ServiceInstanceReference service
  final Map cache = [:]
  
  final Map options = [
    testing : [
      csharp : [
        include : /.*\.[Dd]eployment[Tt]ests?\.csproj/
      ]
    ]
  ]

  TestServiceJob(Map options=[:], pipeline) {
    super(pipeline)
    nexusConfig.with(DefaultValues.nexus)
    k8sConfig.with(DefaultValues.k8s)
    k8s = new K8sClient(pipeline)
    setService(options.service)
  }

  @NonCPS
  void setArtifact(String value) {
    artifact = ArtifactReference.fromText(value)
  }

  @NonCPS
  void setService(String value) {
    service = ServiceInstanceReference.fromText(value)
  }

  @NonCPS
  void setK8sConfig(String value) {
    k8sConfig.configRef = value
  }

  void initializeJob() {
    k8s.configPath = k8sConfig.configPath
    k8s.context = service.env.context
  }

  def defineEnvVars() {
    k8sConfig.defineEnvVars(pipeline)
  }

  def defineVolumes() {
    k8sConfig.defineVolumes(pipeline)
  }

  def run() {
    initialize()
    test()
  }

  def initialize() {
    stage('initialize') {
      if (!artifact) {
        final def tag = k8s.getServicePodLabel(service.name, 'scmTag', namespace: service.env.namespace)
        artifact = "${ service.name } : ${ tag }"
      }

      printReport(
        artifact: artifact,
        service: service,
        k8sConfig: k8sConfig,
        nexus: nexusConfig
      )
    }
  }

  def test() {
    stage('test') {
      final env = [
        "SERVICE_HOST=${ serviceHost.url - 'https://' }",
        "SERVICE_HOST_CREDS=${ serviceHost.creds }",
        "SERVICE_URL=${ serviceUrl }"
      ]
      dotnetTest(env)
    }
  }

  private String getIngressUrl() {
    if (cache.ingressUrl == null) {
      final url = k8s.getResourceData('service', ['e4d-rproxy'],
        namespace: 'kube-system',
        output: 'go-template',
        template: '{{ index .metadata.annotations "external-dns.alpha.kubernetes.io/hostname" }}',
        ignore_not_found: true,
      ) ?: ''
      cache.ingressUrl = url.contains('<no value>') ? '' : url
    }
    return cache.ingressUrl
  }

  private String getServiceSelfLink() {
    if (cache.serviceSelfLink == null) {
      final link = k8s.getResourceData('service', [service.name],
        namespace: service.env.namespace,
        output: 'go-template={{ .metadata.selfLink }}'
      ) ?: ''
      cache.serviceSelfLink = link.contains('<no value>') ? '' : link
    }
    return cache.serviceSelfLink
  }

  private Map getServiceHost() {
    if (cache.serviceHost == null) {
      if (ingressUrl) {
        cache.serviceHost = [url: ingressUrl, creds: 'none']
      } else {
        cache.serviceHost = k8s.getServer(service.env.context)
      }
    }
    return cache.serviceHost
  }

  private String getServiceUrl() {
    if (cache.serviceUrl == null) {
      if (ingressUrl) {
        cache.serviceUrl = [
          '',
          service.env.namespace,
          service.name,
        ].join('/')
      } else {
        cache.serviceUrl = [
          serviceSelfLink,
          'http/proxy',
        ].join(':')
      }
    }
    return cache.serviceUrl
  }

  def dotnetTest(env) {
    pipeline.withCredentials([pipeline.usernamePassword(
      credentialsId: nexusConfig.credsId, usernameVariable: 'user',
      passwordVariable: 'password')]) {
      def image = [nexusConfig.authority, [artifact.name,
        [artifact.tag, 'build'].join('-')].join(':')].join('/')
      def shell = new DockerImageShell(new PipelineShell(pipeline), image,
        registry: nexusConfig.authority , user: pipeline.env.user,
        password: pipeline.password, env: env, network: 'host')
      def dotnet = new DotnetTool(pipeline, shell)
      dotnet.test('.', includeProjects: options.testing.csharp.include)
    }
  }
}
