#!groovy

def call(
    BRANCH_NAME,
    EnVars = [],
    body
)
{

    def JENKINS_NAMESPACE = "jenkins"
    
    def envVars = [
        envVar(key: 'PROJECT_PATH',              value: "${PROJECT_PATH}"),
        envVar(key: 'BUILD_CONFIGURATION',       value: "${BUILD_CONFIGURATION}"),
        envVar(key: 'GITSCM_REPO_URL',           value: "${GITSCM_REPO_URL}"),
        envVar(key: 'GITSCM_CREDSID',            value: "${GLOBAL_GITSCM_CREDSID}"),
        envVar(key: 'BRANCH_NAME',               value: "${BRANCH_NAME}"),
        envVar(key: 'JOB_BASE_NAME',             value: "${JOB_BASE_NAME}"),
        envVar(key: 'BUILD_ID',                  value: "${BUILD_ID}"),        
        envVar(key: 'RUN_TESTS',                 value: "${RUN_TESTS}"),           
        envVar(key: 'JENKINS_NAMESPACE',         value: "${JENKINS_NAMESPACE}"),
        envVar(key: 'NEXUS_NUGET_REPO_URL',      value: "${GLOBAL_NEXUS_NUGET_REPO_URL}"),  
        envVar(key: 'DO_PUBLISH_NUGET',          value: "${DO_PUBLISH_NUGET}"),
        secretEnvVar(key: 'NUGET_CONFIG',        secretName: 'infra-secretconfigs', secretKey: 'nuget.config'),
        secretEnvVar(key: 'NEXUS_APIKEY',        secretName: 'infra-credentials',   secretKey: 'nexus.apikey')
           
    ] + EnVars
    
    podTemplate_basic("${JENKINS_NAMESPACE}", envVars){
        node("slv_${JOB_BASE_NAME}_${BUILD_ID}".toLowerCase()) {
            stage('Pull') {
              checkoutSCM("${BRANCH_NAME}", "${GLOBAL_GITSCM_CREDSID}", "${GITSCM_REPO_URL}")
            }

            container('dotnetcore-sdk') {
                stage('Pack') {
                    sh '''
                        SOURCEROOT=$(pwd) && cd $PROJECT_PATH
                        set +x && echo $NUGET_CONFIG > $SOURCEROOT/NuGet.Config && set -x
                        dotnet restore --configfile $SOURCEROOT/NuGet.Config
                        dotnet pack --include-symbols --configuration $BUILD_CONFIGURATION --output $SOURCEROOT/packages /p:SourceLinkCreate=true 
                        find $SOURCEROOT/packages -type f ! -name '*.symbols.nupkg' -delete
                        pwsh -c 'Get-ChildItem '"$SOURCEROOT"'/packages/* | Rename-Item -NewName {$_.Name -replace ".symbols"}'
                    '''
                }

                if (RUN_TESTS == "true") {  
                    stage('Test') {
                        sh '''
                            SOURCEROOT=$(pwd) && cd $PROJECT_PATH
                            dotnet test */*.Tests.csproj  --results-directory $SOURCEROOT --logger:trx 
                            mv $SOURCEROOT/*.trx $SOURCEROOT/TestResults.trx
                        '''
                    }
                    step([
                        $class: 'MSTestPublisher',
                        testResultsFile:"TestResults.trx",
                        failOnError: true,
                        keepLongStdio: true
                    ])
                }

                if (DO_PUBLISH_NUGET == "true") {  
                    stage('Push') {
                        sh '''
                            set +x && for item in `ls packages`; do dotnet nuget push packages/$item -k $NEXUS_APIKEY -s $NEXUS_NUGET_REPO_URL; done && set -x
                        '''
                    }
                }
                else {
                    sh "echo Publish is skipped because 'DO_PUBLISH_NUGET' is not set to true"
                }
            }
        }
        body()
    }
}
