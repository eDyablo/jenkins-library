package com.e4d.template

import com.cloudbees.groovy.cps.NonCPS

class MapMerger {
  @NonCPS
  def merge(first, second) {
    second.each { key, value ->
      if (first[key] in Map) {        
        first[key] = merge(first[key], value)
      } else {
        first[key] = value
      }
    }
    first
  }

  @NonCPS
  def merge(List sets) {
    sets.inject([:]) { first, second -> 
      merge(first, second)
      first
    }
  }
}
