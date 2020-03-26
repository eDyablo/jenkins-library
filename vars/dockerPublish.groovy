#!groovy

def call(Map args) {
  final String projectDir = args.projectDir
  final String version = args.version
  final String serviceImage = args.serviceImage
  final String nexusRepoUrl = args.nexusRepoUrl ?: GLOBAL_NEXUS_REPO_URL  
  final String nexusRepoShortUrl = nexusRepoUrl - 'http://'
  final String imageName ="${nexusRepoShortUrl}/${serviceImage}".toLowerCase()
  final String fullImageName ="${imageName}:${version}"
  final String latestImageName ="${imageName}:latest"
  if (args.beforePublish) {
    args.beforePublish()
  }
  container('docker') {
    sh """
      set +x && echo \$NEXUS_PASSWORD | docker login -u \$NEXUS_USER --password-stdin ${nexusRepoUrl} && set -x
      docker build --network=host -f ${projectDir}/Dockerfile -t ${fullImageName} .
      docker push ${fullImageName}
      docker tag ${fullImageName} ${latestImageName}
      docker push ${latestImageName}
      docker images -a | grep ${imageName} | grep ${version} | awk '{print \$3}' | xargs docker rmi -f
    """
  }
  if (args.afterPublish) {
    args.afterPublish()
  }
  return [
    repository: nexusRepoShortUrl,
    name: serviceImage.toLowerCase(),
    tag: version
  ]
}
