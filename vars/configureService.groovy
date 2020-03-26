#!groovy

def call(Map args) {
  final String serviceName = args.serviceName.toLowerCase()
  final String branch = args.branch
  final String gitTag = args.gitTag
  final String sourceNamespace = args.sourceNamespace
  final String targetNamespace = args.targetNamespace
  final String pipRepoUrl = args.pipRepoUrl - 'http://'
  final String kubeContext = args.kubeContext ?: 'dev'
  final String jobName = args.jobName ?: JOB_BASE_NAME
  final String buildId = args.buildId ?: BUILD_ID
  final String repoUrl = args.repoUrl ?: GITSCM_REPO_URL
  final String repoCredentialsId = args.repoCredentialsId ?: GLOBAL_GITSCM_CREDSID
  def secretOverrides = args.secretOverrides ?: ''
  def podEnvVars = [
    args.kubeConfig.var, args.nexusUser.var, args.nexusPassword.var
  ]
  final String nodeName = "slv_${jobName}_${buildId}".toLowerCase()
  final String sourceResourcePrefix = serviceName
  final String targetResourcePrefix = "${serviceName}-${gitTag}"
  podTemplate_basic(sourceNamespace, podEnvVars) {
    node(nodeName) {
      echo("""
        Configure using '${args.configEnv}' environment
        Source resources '${sourceResourcePrefix}' in '${sourceNamespace}'
        Target resources '${targetResourcePrefix}' in '${targetNamespace}'""")
      checkoutSCM(branch, repoCredentialsId, repoUrl)
      container('python') {

        k8s.configure(
          context: kubeContext,
          config: args.kubeConfig
        )

        installConfigurationTools(
          repoUrl: pipRepoUrl,
          user: args.nexusUser,
          password: args.nexusPassword
        )

        createConfigMap(
          namespace: targetNamespace,
          configDir: args.configDir,
          prefix: targetResourcePrefix,
          env: args.configEnv
        )

        def infraSecretsNames = ['infra-secretconfigs', 'infra-urls',
          'infra-credentials', 'regsecret']
        infraSecretsNames.each {
          def secret = k8s.getSecret(it, sourceNamespace)
          secret.data.metadata.namespace = targetNamespace
          k8s.applyResource(secret)
        }

        def serviceSecret = k8s.getSecret("${sourceResourcePrefix}-secret", sourceNamespace)
        serviceSecret.data.metadata.guid = null
        serviceSecret.data.metadata.name = "${targetResourcePrefix}-secret"
        serviceSecret.data.metadata.namespace = targetNamespace
        k8s.applyResource(serviceSecret)

        patchSecret(
          serviceSecret.data.metadata.name,
          'secret.json',
          secretOverrides,
          namespace: targetNamespace
        )
      }
    }
  }
}

def installConfigurationTools(Map args) {
  final String repoUrl = args.repoUrl
  final String repoUrlBase = repoUrl.split('/')[0]
  return shell.execute("""
    set +x
    pip3 download configandsecrets -i "http://\$${args.user.key}:\$${args.password.key}@${repoUrl}/simple" --trusted-host ${repoUrlBase}
    set -x
    mkdir ./configandsecrets && tar -xf *.tar.gz -C ./configandsecrets  && mv ./configandsecrets/*/* ./configandsecrets
  """)
}

def createConfigMap(Map args) {
  final String mapName = "${args.prefix}-config"
  final String envMapName = "${args.prefix}-env"
  return shell.execute("""
    kubectl delete configmap ${mapName} ${envMapName} --namespace=${args.namespace} --ignore-not-found
    python configandsecrets/create_config.py ${args.configDir} --map-name=${mapName} --env-map-name=${envMapName} --env=${args.env} | kubectl create --namespace=${args.namespace} --filename=-
  """)
}

def patchSecret(Map kwargs = [:], String name, String member, String patch) {
  return patchConfigResource(kwargs + [kind: 'secret', name: name, member: member, patch: patch])
}

def patchConfigMap(Map kwargs = [:], String name, String member, String patch) {
  return patchConfigResource(kwargs + [kind: 'configmap', name: name, member: member, patch: patch])
}

def patchConfigResource(Map kwargs) {
  final String namespace = kwargs.namespace ?: 'default'
  final String name = kwargs.name
  final String kind = kwargs.kind
  final String member = kwargs.member
  final String patch = kwargs.patch
  return shell.execute("""
    set +x
    python configandsecrets/patch_config.py --namespace ${namespace} ${kind} ${name} ${member} ${patch}
    set -x""")
}
