#!groovy

def call(
    BRANCH_NAME,
    GITSCM_CREDSID,
    GITSCM_REPO_URL
)
{

    if (BRANCH_NAME ==~ /origin\/pr\/.*/){
      checkout([
          $class: 'GitSCM',
          branches: [[name: "${BRANCH_NAME}"]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [],
          submoduleCfg: [],
          userRemoteConfigs: [[
              credentialsId: "${GITSCM_CREDSID}",
              name: 'origin',
              refspec: '+refs/pull/*:refs/remotes/origin/pr/*',
              url: "${GITSCM_REPO_URL}"
          ]]
      ])
    }
    else{
      checkout([
          $class: 'GitSCM',
          branches: [[name: "${BRANCH_NAME}"]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [],
          submoduleCfg: [],
          userRemoteConfigs: [[
              credentialsId: "${GITSCM_CREDSID}",
              url: "${GITSCM_REPO_URL}"
          ]]
      ])
    }
}    
