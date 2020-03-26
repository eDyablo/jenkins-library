import com.e4d.build.BuildContext
import com.e4d.build.Path
import com.e4d.git.GitPath
import com.e4d.docker.DockerClient
import com.e4d.docker.DockerImageName

def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  properties([
    parameters([
      string(name: 'DOCKERFILE_PATH', defaultValue: config.dockerfilePath ?: '/Dockerfile', description: 'Path to dockerfile relative to git base url'),
      string(name: 'IMAGE_NAME', defaultValue: config.imageName, description: 'Name and tag for the image being produced'),
      string(name: 'GIT_BASE_URL', defaultValue: config.gitBaseUrl ?: GLOBAL_GITSCM_BASE_URL, description: 'URL to Git repository'),
      string(name: 'GIT_CREDS_ID', defaultValue: config.gitGredsId ?: GLOBAL_GITSCM_CREDSID, description: 'Reference to Git credentials stored in Jenkins'),
      string(name: 'GIT_BRANCH', defaultValue: config.gitBranch ?: 'develop', description: 'Name of the branch'),
    ])
  ])
  def buildContext = new BuildContext(this)
  podTemplate(label: buildContext.slaveLabel(),
    annotations:  [
      podAnnotation(key: "iam.amazonaws.com/role", value: "RoleCiSlave")
    ],
    containers: [
      containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true),
    ],
    volumes: [
      hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
    ]) {
    node(buildContext.slaveLabel()) {
      stage('Build') {
        def imageName = DockerImageName.fromText(params.IMAGE_NAME)
        imageName.tag = imageName.tag.append(Integer.parseInt(BUILD_ID))

        def source = GitPath.fromText(params.DOCKERFILE_PATH)

        def checkout = checkoutRecentSource(
          repository: source.repository
        )

        def docker = new DockerClient(this)
        docker.buildImage(source.fullPath(), imageName)
      }
    }
  }
}
