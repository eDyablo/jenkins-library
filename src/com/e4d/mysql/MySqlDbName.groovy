package com.e4d.mysql

import com.e4d.build.NameUtils
import com.cloudbees.groovy.cps.NonCPS

class MySqlDbName {
  String name
  
  static def pattern = ~/[aA][hH][0-9a-fA-F]{8}[tT]\d{6}[0-9a-fA-F]{3}/
  
  static MySqlDbName fromText(String text) {
    return new MySqlDbName(name: text)
  }
  
  MySqlDbName(String key, String postfix = null) {
    name = "AH${NameUtils.shortMinutelyUniqueName(key)}${postfix ? '-' : ''}${postfix ?: ''}"
  }
  
  String getHash() {
    return name.substring(2, 10)
  }

  String getDate() {
    return name.substring(11, 17)
  }
  
  @NonCPS
  @Override
  String toString() {
    return name
  }

  private MySqlDbName(Map args) {
    name = args.name
  }
}
