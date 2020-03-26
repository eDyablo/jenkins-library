package com.e4d.build

import com.cloudbees.groovy.cps.NonCPS
import java.util.Date
import java.util.TimeZone

class NameUtils {
  @NonCPS
  static long longHash(String name) {
    return Integer.toUnsignedLong(name.hashCode())
  }

  @NonCPS
  static int shortHash(String name) {
    long longHash = longHash(name)
    int shortHash = (longHash ^ (longHash >> 32))
    return shortHash
  }

  @NonCPS
  static String longHashedName(String key) {
    return Long.toHexString(longHash("${key}")).padLeft(16, '0')
  }

  @NonCPS
  static String shortHashedName(String key) {
    return Integer.toHexString(shortHash("${key}")).padLeft(8, '0')
  }

  @NonCPS
  static String shortDailyUniqueName(String key) {
    String date = new Date().format('yyMMdd', TimeZone.getTimeZone('UTC')).padLeft(6, '0')
    return "${shortHashedName(key)[0..7]}T${date[0..5]}"
  }

  @NonCPS
  static String shortMinutelyUniqueName(String key) {
    Date currentTime = new Date()
    int hours = currentTime.getAt(Calendar.HOUR_OF_DAY)
    int minutes = currentTime.getAt(Calendar.MINUTE)
    int minutesOfDay = hours * 60 + minutes
    String time = Integer.toHexString(minutesOfDay).padLeft(3, '0')
    return "${shortDailyUniqueName(key)}${time[0..2]}"
  }

  @NonCPS
  static String shortTimelyUniqueName(String key) {
    def now = java.time.Instant.now()
    def time = Long.toHexString(now.toEpochMilli())
    "${shortHashedName(key)[0..7]}t$time"
  }
}
