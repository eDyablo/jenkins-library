#!groovy

def call(Map args) {
  final String projectDir = args.projectDir
  final String testProjectPattern = args.testProjectPattern ?: '*.csproj'

  container('dotnetcore-sdk') {
    sh """
      set +x && echo "\$NUGET_CONFIG" > ${projectDir}/NuGet.Config && set -x
      dotnet restore ${projectDir} --configfile ${projectDir}/NuGet.Config
      ls ${projectDir}/${testProjectPattern} | xargs -L1 dotnet test --no-restore --results-directory . --logger:trx
    """

    step([
      $class: 'MSTestPublisher',
      testResultsFile: "**/*.trx",
      failOnError: false,
      keepLongStdio: true
    ])
  }
}
