package com.e4d.job

import com.e4d.k8s.*
import com.e4d.nuget.*
import com.e4d.pip.*

class SandboxJob extends PipelineJob {
  Closure action
  final K8sClient k8sClient
  K8sConfig k8sConfig = new K8sConfig()
  NugetConfig nugetConfig = new NugetConfig()
  PipConfig pipConfig = new PipConfig()

  SandboxJob(pipeline) {
    super(pipeline)
    k8sConfig.with(DefaultValues.k8s)
    nugetConfig.with(DefaultValues.nuget)
    pipConfig.with(DefaultValues.pip)
    k8sClient = new K8sClient(pipeline)
  }

  def loadParameters(params) {
    k8sConfig.loadParameters(params)
  }

  void initializeJob() {
    k8sClient.configPath = k8sConfig.configPath
  }

  def defineEnvVars() {
    k8sConfig.defineEnvVars(pipeline).findAll() +
    nugetConfig.defineEnvVars(pipeline).findAll() +
    pipConfig.defineEnvVars(pipeline).findAll()
  }

  def defineVolumes() {
    k8sConfig.defineVolumes(pipeline).findAll() +
    nugetConfig.defineVolumes(pipeline).findAll() +
    pipConfig.defineVolumes(pipeline).findAll()
  }

  def run(Closure closure) {
    action = closure
  }

  def run() {
    action.delegate = pipeline
    action.resolveStrategy = Closure.DELEGATE_FIRST
    action.call()
  }
}
