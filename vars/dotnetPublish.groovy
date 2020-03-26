#!groovy

def call(Map args) {
  final String projectDir = args.projectDir
  final String buildConfig = args.buildConfig ?: BUILD_CONFIGURATION

  if (args.beforePublish) {
    args.beforePublish()
  }

  container('dotnetcore-sdk') {
    sh """
      set +x && echo \$NUGET_CONFIG > ${projectDir}/NuGet.Config && set -x
      dotnet restore ${projectDir}/*.csproj --configfile ${projectDir}/NuGet.Config
      dotnet publish ${projectDir} -c ${buildConfig} -o \$(pwd)/obj/Docker/publish --no-restore /p:Environment=K8S
    """
  }

  if (args.afterPublish) {
    args.afterPublish()
  }
}
