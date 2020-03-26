package com.e4d.job

import com.e4d.build.*
import com.e4d.git.GitConfig
import com.e4d.k8s.*


class DataPreparationJob extends DeployServiceJob {

  String branch = 'develop'
  boolean useMigration = true
  String dbCredentialId = 'performanceDB'
  String migrationFile = '*.Web.Data.Migrations.csproj'
  String migrationFolder = 'src/*.Web.Data.Migrations'
  String dumpFile = 'load_test/migration/migration_schemas.sql'
  String nugetConfig = 'home/jenkins/.nuget/NuGet/NuGet.Config'

  GitConfig gitConfig = new GitConfig()
  final TemplateEngine templateEngine

  DataPreparationJob(pipeline){
    super(pipeline)
    gitConfig.with(DefaultValues.git)
    gitConfig.branch = branch
    templateEngine = new TemplateEngine(pipeline)
  }


  def run(){
    initEnvVars(pipeline)

    // Checkout Steps
    checkoutGitRepo(artifact.name, branch)

    if (useMigration){
      prepareFromMigration()
    } else {
      if (pipeline.env.MYSQL_CONNECTION_STRING){
        prepareFromSQL(pipeline.env.MYSQL_CONNECTION_STRING)
      } else {
        prepareFromSecretManager(values.deployment.environment)
      }
    }

    // Run Deployment Job
    super.run()
  }

  def checkoutGitRepo(String repoName, String branch){
    stage("checkout git"){
      pipeline.git branch: branch, credentialsId: gitConfig.credsId, url: "${gitConfig.baseUrl}/${repoName}.git"
    }
    // Delete config and deploy folders before 'pull()' stage.
    pipeline.sh 'rm -rf deploy config'
  }

  def initEnvVars(pipeline){
    def creds = [
      pipeline.string(credentialsId: dbCredentialId, variable: 'dbConnectionString')
    ]
    pipeline.withCredentials(creds){
      pipeline.env.MYSQL_CONNECTION_STRING = pipeline.env.dbConnectionString
      if (artifact.name == 'svc-medbills'){
        pipeline.env.MedbillsMySqlAdminConnectionString = pipeline.env.dbConnectionString
      }
    }
    if (migrationFile) { pipeline.env.MIGRATION_FILE = migrationFile }
  }

  def prepareFromSecretManager(envName){
    stage('prepare database'){
      pipeline.echo "Inserting Database Dump from secret: $envName"
      def env = [
        "ENV_NAME=${envName}",
        "MIGRATION_FILE=${migrationFile}",
      ]
      pipeline.withEnv(env){
        pipeline.sh '''
        cd load_test/migration
        python start_migration.py
        '''
      }
    }
  }

  def prepareFromSQL(connectionString){
    stage('prepare database') {
      pipeline.echo "Inserting Database Dump. from sql file: ${dumpFile}"
      def conString = connectionStringToMap(connectionString)
      def startTime = System.currentTimeMillis()
      def command = String.format('mysql -u %s -p"%s" --host %s --port %s < %s', conString.username, conString.password, conString.host, conString.port, dumpFile)
      pipeline.sh('#!/bin/sh -e\n' + command)
      def endTime = (System.currentTimeMillis() - startTime) / 1000
      pipeline.echo "Dump Duration time: ${String.format("%.2f", endTime)} sec."
    }
  }

  def prepareFromMigration(){
    stage('prepare database'){
      pipeline.echo "Inserting Database Dump from .NET Migration"
      pipeline.sh """
        cd ${migrationFolder}
        export PATH="\$PATH:\$HOME/.dotnet/tools/"
        export PATH=\$PATH:/home/jenkins/.dotnet/tools/
        dotnet restore --configfile /${nugetConfig}
        dotnet ef database update --project ${migrationFile}
       """
    }
  }

  static def connectionStringToMap(String connectionString){
    def connectionStringMap = [:]// new HashMap<String, String>()
    def credentials = connectionString.split(';')
    credentials.each { cred ->
      def args = cred.split("=")
      connectionStringMap.put(args[0].toString(), args[1].toString())
    }
    return connectionStringMap
  }


}
