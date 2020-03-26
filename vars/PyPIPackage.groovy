#!groovy

def call(
    BRANCH_NAME,
    EnVars = [],
    body
)
{
    def JENKINS_NAMESPACE = "jenkins"
    
    def envVars = [
        envVar(key: 'PROJECT_PATH',        value: "${PROJECT_PATH}"),
        envVar(key: 'GITSCM_REPO_URL',     value: "${GITSCM_REPO_URL}"),
        envVar(key: 'GITSCM_CREDSID',      value: "${GLOBAL_GITSCM_CREDSID}"),
        envVar(key: 'PYPI_REPO_NAME',      value: "${PYPI_REPO_NAME}"),
        envVar(key: 'BRANCH_NAME',         value: "${BRANCH_NAME}"),
        envVar(key: 'JOB_BASE_NAME',       value: "${JOB_BASE_NAME}"),
        envVar(key: 'BUILD_ID',            value: "${BUILD_ID}"),        
        envVar(key: 'JENKINS_NAMESPACE',   value: "${JENKINS_NAMESPACE}"),            
        secretEnvVar(key: 'PYPIRC_CONFIG', secretName: 'infra-secretconfigs', secretKey: 'pypi.config')
    ] + EnVars
    
    podTemplate_basic("${JENKINS_NAMESPACE}", envVars){
        node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
   
            stage('Pull') {
              checkoutSCM("${BRANCH_NAME}", "${GLOBAL_GITSCM_CREDSID}", "${GITSCM_REPO_URL}")
            }

            container('python') {
                stage('Test') {
                    sh '''
                        cd $PROJECT_PATH
                        python -m unittest discover -p '*_test.py' -v
                    '''
                }

                if (BRANCH_NAME == "develop") {  
                    stage('Push') {
                        sh '''
                            cd $PROJECT_PATH
                            set +x && echo "$PYPIRC_CONFIG" > ~/.pypirc && set -x
                            python3 setup.py sdist upload -r $PYPI_REPO_NAME
                        '''
                    }
                }
                else {
                    sh "echo Publish is skipped because branch is not 'develop' but ${BRANCH_NAME}"
                }
            }
        }
        body()
    }
}
