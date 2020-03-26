#!groovy

def call(Map args) {
  final String podNamespace = args.podNamespace
  final String serviceNamespace = args.serviceNamespace
  final String templatePath = args.templatePath
  final String gitTag = args.gitTag
  final String kubeContext = args.kubeContext ?: 'dev'
  final String jobName = args.jobName ?: JOB_BASE_NAME
  final String buildId = args.buildId ?: BUILD_ID
  final String repoUrl = args.repoUrl ?: GITSCM_REPO_URL
  final String repoCredentialsId = args.repoCredentialsId ?: GLOBAL_GITSCM_CREDSID
  final Boolean isDebug = args.isDebug ?: false
  final String configPath = args.configPath ?: AH_CONFIGURATION_PATH
  final String configApp = args.configApp ?: AH_CONFIGURATION_APP
  final String configPrefix = args.configPrefix
  final String serviceName = args.serviceName.toLowerCase()
  final String serviceImage = args.serviceImage.toLowerCase()
  final String imageStorageUrl = GLOBAL_NEXUS_DOCKER_REGISTRY_URL
  def templateVars = [
    envVar(key: 'AH_CONFIGURATION_APP', value: configApp),
    envVar(key: 'AH_CONFIGURATION_DEBUG', value: isDebug.toString()),
    envVar(key: 'AH_CONFIGURATION_ENV', value: kubeContext),
    envVar(key: 'AH_CONFIGURATION_PATH', value: configPath),
    envVar(key: 'AH_SCM_TAG', value: gitTag),
    envVar(key: 'AH_SERVICE_ENVIRONMENT', value: kubeContext),
    envVar(key: 'AH_SERVICE_NAME', value: args.serviceName),
    envVar(key: 'AH_SERVICE_SUB_ENVIRONMENT', value: serviceNamespace),
    envVar(key: 'AH_CLUSTER', value: kubeContext),
    envVar(key: 'AH_NAMESPACE', value: serviceNamespace),
    envVar(key: 'GITSCM_TAG', value: gitTag),
    envVar(key: 'GITSCM_TAG_NO_DOTS', value: gitTag.replaceAll('\\.', '-')),
    envVar(key: 'NEXUS_DOCKER_REGISTRY_URL', value: imageStorageUrl),
    envVar(key: 'SERVICE_IMAGE_LOWER', value: serviceImage),
    envVar(key: 'SERVICE_NAME_LOWER', value: serviceName)
  ]
  def podEnvVars = [
    args.kubeConfig.var, args.nugetConfig.var
  ] + templateVars
  final String nodeName = "slvm_${jobName}_${buildId}".toLowerCase()
  podTemplate_migration(jobName, buildId, serviceNamespace, configPath, configPrefix, podEnvVars) {
    node(nodeName) {
      echo("Deploy '${serviceImage}' tagged as '${gitTag}' from '${imageStorageUrl}'")
      checkout([
        $class: 'GitSCM',
        branches: [[name: "tags/${gitTag}"]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [],
        submoduleCfg: [],
        userRemoteConfigs: [[credentialsId: "${repoCredentialsId}", url: "${repoUrl}"]]
      ])
      if (args.beforeDeploy) {
        args.beforeDeploy()
      }
      container('dotnetcore-sdk') {
        shell.execute("""
          set +x && echo "\$${args.nugetConfig.key}" > NuGet.Config && set -x
          cat NuGet.Config
          chmod +x ${args.predeploymentScriptPath} && ${args.predeploymentScriptPath}
        """)
      }
      container('kube') {
        sh """
          GITSCM_TAG=${gitTag}
          echo "checking variables in YAML template:"
          for placeholder in \$(grep -wo '\${[A-Z_]*}' ${templatePath} | sort | uniq);
          do
            variable=\$(echo \$placeholder | sed 's/{//' | sed 's/}//')
            variable_value=\$NULL
            eval "variable_value=\$variable"
            if [ -z "\$variable_value" ]; then
              echo "FAILED \$variable_value is empty or not passed via params" 1>&2
              exit 1
            else
              sed -i "s#\$placeholder#\$variable_value#g" ${templatePath}
            fi
          done
          echo "all placeholders are replaced"

          echo "configuring kube config to work with needed context"
          mkdir ~/.kube && set +x && echo "\$${args.kubeConfig.key}" > ~/.kube/config && set -x

          kubectl config use-context ${kubeContext}

          echo "applying kubernetes deployment"
          kubectl apply -f ${templatePath} --namespace=${serviceNamespace}
        """
      }
      if (args.afterDeploy) {
        args.afterDeploy()
      }
    }
  }
}
