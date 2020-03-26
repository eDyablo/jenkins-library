import com.cloudbees.groovy.cps.NonCPS
import org.jenkinsci.plugins.pipeline.modeldefinition.*

def source
Map options = [:]

def call(Map options=[:], Closure code={}) {
  stage('initialize') {
    initialize(options, code)
  }
  node {
    stage('checkout') {
      checkoutSource()
    }
    stage('generate') {
      if (isForced()) {
        generateClient()
      } else if (isSwaggerSpecChanged()) {
        generateClient()
      } else {
        skipStage("There are no changes in public API interface")
      }
    }
  }
}

def initialize(Map options, Closure code) {
  this.options = checkOptions(options)
  printOptions(this.options)
  code.delegate = this.options
  code()
}

def checkOptions(Map options) {
  if (!options.service) {
    error('Name of the service is required')
  }
  if (!options.gitRepository) {
    error('Git repository is required')
  }
  options.gitBranch = options.gitBranch ?: 'develop'
  options.gitCredentialsId = options.gitCredentialsId ?: 'e4d-github-ci'
  options.version = options.version ?: '1.0.0'
  options.swaggerSpec = options.swaggerSpec ?: 'swagger.json'
  options.versionTag = options.versionTag ?: "${ options.version }.${ BUILD_ID }"
  options.generatorJob = options.generatorJob ?: '/e4d/PostMergeJobs/HttpClients/HttpClientGenerator'
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

def checkoutSource() {
  source = checkout([
    $class: 'GitSCM', 
    branches: [[name: "refs/heads/${ options.gitBranch }"]], 
    doGenerateSubmoduleConfigurations: false,
    extensions: [], 
    submoduleCfg: [], 
    userRemoteConfigs: [[
      credentialsId: options.gitCredentialsId,
      refspec: "+refs/heads/${ options.gitBranch }:refs/remotes/origin/${ options.gitBranch }",
      url: options.gitRepository
      ]]
    ])
}

boolean isForced() {
  return options.force ?: false
}

boolean isSwaggerSpecChanged() {
  def log = [:]
  try
  {
    log['recent commit'] = source.GIT_COMMIT
    log['previous commit'] = source.GIT_PREVIOUS_SUCCESSFUL_COMMIT
    if (source.GIT_PREVIOUS_SUCCESSFUL_COMMIT && (source.GIT_PREVIOUS_SUCCESSFUL_COMMIT != source.GIT_COMMIT)) {
      try {
        def changesCount = sh(
          script: "git log --name-only --pretty=oneline --full-index ${ source.GIT_PREVIOUS_SUCCESSFUL_COMMIT }..${ source.GIT_COMMIT } | grep -c ${ options.swaggerSpec }",
          returnStdout: true) as Integer
        log['commits count'] = changesCount
        return changesCount > 0
      } catch (any) {
        return false
      }
    }
    return false
  }
  finally {
    printOptions(log)
  }
}

def skipStage(String message) {
  println message
  Utils.markStageSkippedForConditional(STAGE_NAME)
}

def generateClient() {
  build(job: options.generatorJob,
    parameters: [
      [$class: 'StringParameterValue', name: 'E4D_SWAGGERCPEC_PATH', value: options.swaggerSpec],
      [$class: 'StringParameterValue', name: 'E4D_SERVICENAME', value: options.service],
      [$class: 'StringParameterValue', name: 'E4D_API_VERSION', value: options.versionTag],
      [$class: 'StringParameterValue', name: 'GITSCM_SERVICE_REPOURL', value: options.gitRepository],
      [$class: 'StringParameterValue', name: 'GITSCM_SERVICE_BRANCH', value: options.gitBranch],
    ])
}
