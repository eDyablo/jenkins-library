import groovy.json.JsonSlurperClassic
import com.cloudbees.groovy.cps.NonCPS
import org.jenkinsci.plugins.pipeline.modeldefinition.*

Map options = [:]

def call(Map options=[:], Closure code={}) {
  stage('initialize') {
    initialize(options, code)
  }
  node {
    stage('download') {
      getSwaggerSpecVersion(options)
    }
    stage('generate') {
      if (isForced()) {
        generateClient()
      } else if (doesClientVersionExist()) {
        generateClient()
      } else {
        skipStage("There are no changes in swagger version.")
      }
    }
  }
}

def initialize(Map options, Closure code) {
  this.options = checkOptions(options)
  code.delegate = this.options
  code()
  printOptions(this.options)
}

def checkOptions(Map options) {
  if (!options.service) {
    error('Name of the service is required')
  }
  if (!options.gitRepository) {
    error('Git repository is required')
  }
  if(!options.artifactName) {
    error('Artifact name is required')
  }
  if(!options.swaggerSpec) {
    error('Swagger url is required')
  }
  options.nexusCredId = options.nexusCredId ?:  params.NEXUS_CREDS_ID
  options.gitBranch = options.gitBranch ?: 'develop'
  options.gitCredentialsId = options.gitCredentialsId ?: 'e4d-github-ci'
  options.generatorJob = options.generatorJob ?: '/e4d/PostMergeJobs/HttpClients/HttpClientFromUrlGenerator'
  options.nexusUrl = options.nexusUrl ?: 'https://artifacts.k8s.us-west-2.dev.e4d.com'
  options.artifactRepository = options.artifactRepository ?: 'debug-nugets'
  options.version = options.version ?: getSwaggerSpecVersion(options)
  return options
}

@NonCPS
def maxLength(list) {
  list*.length().max()
}

def printOptions(Map options) {
  def width = maxLength(options.keySet()) + 1
  println options.collect { key, value ->
    "${ key.padRight(width) }${ value }"
  }.join('\n')
}

def getSwaggerSpecVersion(Map options) {
  try {
    final response = parseJson(options.swaggerSpec.toURL())
    response.info.version
  } catch(Exception ex) {
    error("Unable to parse swagger spec at ${ options.swaggerSpec }")
  }
}

@NonCPS
def parseJson(URL ref) {
  new JsonSlurperClassic().parse(ref.newReader())
}

boolean isForced() {
  return options.force ?: false
}

boolean doesClientVersionExist() {
    withCredentials([usernamePassword(credentialsId: options.nexusCredId,
          usernameVariable: 'username', passwordVariable: 'password')]) {

      String script = "curl -u ${env.username}:${env.password} \"${options.nexusUrl}/service/rest/v1/search?repository=${options.artifactRepository}&name=${options.artifactName}&version=${options.version}\""
      def output = sh(script: script,
              returnStdout: true) as String
      def nexusResponse = new JsonSlurperClassic().parseText(output)

      return nexusResponse.items.size() == 0
    }
}

def skipStage(String message) {
  println message
  Utils.markStageSkippedForConditional(STAGE_NAME)
}

def generateClient() {
  build(job: options.generatorJob,
    parameters: [
      [$class: 'StringParameterValue', name: 'E4D_SWAGGERCPEC_URL', value: options.swaggerSpec],
      [$class: 'StringParameterValue', name: 'E4D_SERVICENAME', value: options.service],
      [$class: 'StringParameterValue', name: 'E4D_API_VERSION', value: options.version],
      [$class: 'StringParameterValue', name: 'GITSCM_SERVICE_REPOURL', value: options.gitRepository],
      [$class: 'StringParameterValue', name: 'GITSCM_SERVICE_BRANCH', value: options.gitBranch],
    ])
}
