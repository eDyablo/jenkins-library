package com.e4d.job

import com.e4d.build.*
import com.e4d.k8s.*
import com.e4d.nexus.*
import com.e4d.service.*
import com.cloudbees.groovy.cps.NonCPS

class ServiceImageHousekeeperJob extends PipelineJob {
  K8sConfig k8s
  NexusConfig nexus
  int versionsToKeep
  NexusClient nexusClient
  K8sClient k8sClient
  ServiceImageRepository imageRepository
  ServiceDeploymentRepository deploymentRepository
  Boolean dryRun = false

  ServiceImageHousekeeperJob(def pipeline) {
    super(pipeline)
    k8s = new K8sConfig()
    nexus = new NexusConfig()
    versionsToKeep = 3
  }

  def defineParameters() {
    return k8s.defineParameters(pipeline)
      .plus(nexus.defineParameters(pipeline))
      .plus([
        pipeline.string(name: 'KEEP_IMAGES_NUMBER', defaultValue: "$versionsToKeep", description: 'Number of recent images to keep for each service')
      ])
  }

  def loadParameters(def params) {
    k8s.loadParameters(params)
    nexus.loadParameters(params)
    versionsToKeep = Integer.parseInt(params.KEEP_IMAGES_NUMBER)
    if (versionsToKeep < 2)
      versionsToKeep = 2
  }

  def defineEnvVars() {
    k8s.defineEnvVars(pipeline)
  }

  def run() {
    initialize()
    def images
    def deployments
    def triagedImages
    pipeline.stage('Collect') {
      images = getImages()
      deployments = getDeployments()
    }
    pipeline.stage('Analyze') {
      triagedImages = triageImages(images, deployments)
      pipeline.echo("Analized ${images.size()} images and ${deployments.size()} deployments")
      pipeline.echo("Keep ${triagedImages.keep.size()} images and drop ${triagedImages.drop.size()} images")
    }
    pipeline.stage('Report') {
      def report = [
        deployed: deployments,
        keep: triagedImages.keep,
        drop: triagedImages.drop
      ]
      pipeline.echo("${buildJobReport(report)}")
    }
    pipeline.stage('Drop') {
      if (!dryRun && triagedImages.drop) {
        imageRepository.deleteImages(triagedImages.drop)
      }
    }
  }

  def initialize() {
    nexusClient = pipeline.nexusClient(nexus)
    k8sClient = new K8sClient(pipeline)
    k8sClient.context = k8s.configRef.context
    imageRepository = new ServiceImageRepository(nexusClient)  
    deploymentRepository = new ServiceDeploymentRepository(k8sClient)
  }

  def getImages() {
    return imageRepository.getAllImages()
  }

  def getDeployments() {
    ['dev', 'production'].collectMany {
      deploymentRepository.withContext(it) { getAllDeployments() }
    }
  }

  /**
   * Triages images to those that can be deleted safely and those that should be kept 
   *
   * @param images a list of images to triage
   * @param deployments a list of deployments to lookup into
   * @returns a tuple contains lists of images accessable via keep and drop members
   */
  @NonCPS
  def triageImages(def images, def deployments) {
    def versionDecreasingOrder = { first, second ->
      second.versionTag <=> first.versionTag
    }
    images.groupBy { it.name }
      .inject([keep: [], drop: []]) { triaged, groupName, groupImages ->
        groupImages.sort(versionDecreasingOrder)
        def groupDeployments = deployments.findAll { it.name == groupName }
        if (!groupDeployments.isEmpty()) {
          groupDeployments.sort(versionDecreasingOrder)
          def indexes = getUsedIndexes(groupImages, groupDeployments,
              versionDecreasingOrder, versionsToKeep)
          def toKeep = []
          def toDrop = []
          mapUsedIndexes(groupImages, indexes, toKeep, toDrop)
          def highestUsedVersion = groupDeployments.first().versionTag
          triaged.keep += groupImages.takeWhile {
            it.versionTag > highestUsedVersion
          } + toKeep
          triaged.drop += toDrop.dropWhile {
            it.versionTag > highestUsedVersion
          }
        }
        else {
          triaged.keep += groupImages
        }
        triaged
      }
  }

  @NonCPS
  def getUsedIndexes(def items, def lookup, def comparator, def markWidth) {
    def usedMarker = [1] * markWidth
    lookup.inject([]) { used, item ->
      def found = Collections.binarySearch(items, item, comparator)
      if (found >= 0)
        used[found..(found + markWidth)] = usedMarker
      used
    }
  }

  @NonCPS
  def mapUsedIndexes(def items, def indexes, def used, def unused) {
    items.inject([count: 0, used: used, unused: unused]) { triaged, item ->
      (indexes[triaged.count++] ? triaged.used : triaged.unused) << item
      triaged
    }
  }

  def buildJobReport(def report, String header = null) {
    (header ? "$header\n" : '') <<
        buildArtifactsReport(report.deployed,
            "Deployed ${report.deployed.size()} images:") << '\n' <<
        buildArtifactsReport(report.keep,
            "Keep ${report.keep.size()} images:") << '\n' <<
        buildArtifactsReport(report.drop,
            "Drop ${report.drop.size()} images:")
  }

  def buildArtifactsReport(def artifacts, String header = null) {
    def lines = artifacts.collect { "$it.name:$it.versionTag $it.tag" }
    (header ? "$header\n" : '') << lines.join('\n')
  }
}
