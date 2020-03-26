package com.e4d.mysql

import com.e4d.mysql.MySqlClient
import com.e4d.mysql.MySqlDbName

class MySqlDbRepository {
  def script
  MySqlClient client

  MySqlDbRepository(def script, MySqlClient client) {
    this.script = script
    this.client = client
  }

  def fetchDatabases() {
    def rawResult = client.execute('SHOW DATABASES;')
    def databases = rawResult.split('\n').drop(1)
    return databases.findAll { it =~ MySqlDbName.pattern }
        .collect { MySqlDbName.fromText(it) }
  }

  def dropDatabases(def databases) {
    def query = databases.collect { "DROP DATABASE `${it}`;" }.join('')
    return client.execute(query)
  }
}
