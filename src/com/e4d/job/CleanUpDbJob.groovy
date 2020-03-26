package com.e4d.job

import com.e4d.build.BuildContext
import com.e4d.mysql.MySqlConfig
import com.e4d.nexus.NexusConfig

class CleanUpDbJob extends PipelineJob {
  MySqlConfig mysql
  NexusConfig nexus
  int daysThreshold

  CleanUpDbJob(def pipeline) {
    super(pipeline)
    mysql = new MySqlConfig()
    nexus = new NexusConfig()
    daysThreshold = 7
  }

  def defineParameters() {
    return mysql.defineParameters(pipeline)
      .plus(nexus.defineParameters(pipeline))
      .plus([
        pipeline.string(name: 'DAYS_THRESHOLD', defaultValue: "${daysThreshold}", description: 'Number of days we keep databases'),
      ])
  }

  def loadParameters(def params) {
    mysql.loadParameters(params)
    nexus.loadParameters(params)
    daysThreshold = Integer.parseInt(params.DAYS_THRESHOLD)
  }

  def run() {
    def repository
    def databases = []
    def toKeep = []
    def toDrop = []
    pipeline.stage('Collect') {
      repository = pipeline.mySqlDbRepository(
          host: mysql.hostUrl, credsId: mysql.credsId)
      databases = repository.fetchDatabases()
      pipeline.echo("${databases.join('\n')}")
    }
    pipeline.stage('Filter') {
      int daysToKeep = daysThreshold
      def jobRelated = databases.groupBy { it.hash }
      jobRelated.each { hash, jobDatabases ->
        jobDatabases.sort { it.date }
        jobDatabases = jobDatabases.reverse()
        def dateRelated = jobDatabases.groupBy { it.date }
        dateRelated.take(daysToKeep).each {
          date, dayDatabases -> toKeep.addAll(dayDatabases)
        }
        dateRelated.drop(daysToKeep).each {
          date, dayDatabases -> toDrop.addAll(dayDatabases)
        }
      }
      pipeline.echo("Keep the following databases:\n${toKeep.join('\n')}")
      pipeline.echo("Drop the following databases:\n${toDrop.join('\n')}")
    }
    pipeline.stage('Drop') {
      repository.dropDatabases(toDrop)
    }
  }
}
