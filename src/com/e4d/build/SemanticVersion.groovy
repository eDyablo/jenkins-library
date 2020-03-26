package com.e4d.build

import com.cloudbees.groovy.cps.NonCPS

class SemanticVersion implements Comparable<SemanticVersion> {
  final short major
  final short minor
  final short patch
  final List<String> buildIds = new ArrayList<String>()
  final List<String> prereleaseIds = new ArrayList<String>()

  static final ZERO = new SemanticVersion([:])

  SemanticVersion(Map options) {
    major = validCoreComponent(options, 'major')
    minor = validCoreComponent(options, 'minor')
    patch = validCoreComponent(options, 'patch')
    buildIds = validIds(options, 'build')
    prereleaseIds = validIds(options, 'prerelease')
  }

  @NonCPS
  static short validCoreComponent(Map options, String name) {
    final component = options?.get(name) ?: 0 as short
    if (component < 0) {
      throw new IllegalArgumentException(
        "${ name.capitalize() } version component is negative")
    }
    return component
  }

  @NonCPS
  static List<String> validIds(Map options, String name) {
    final def value = options?.get(name)
    if (value instanceof List) {
      return value
    }
    if (value) {
      return [value.trim()].findAll()
    }
    return []
  }

  @NonCPS
  String getCore() {
    [major, minor, patch].join('.')
  }

  @NonCPS
  String getRelease() {
    [core, prerelease].findAll().join('-')
  }

  @NonCPS
  String getBuild() {
    buildIds.join('.')
  }

  @NonCPS
  String getPrerelease() {
    prereleaseIds.join('.')
  }

  @NonCPS
  @Override String toString() {
    [release, build].findAll().join('+')
  }

  @Override int compareTo(SemanticVersion other) {
    major <=> other.major ?:
    minor <=> other.minor ?:
    patch <=> other.patch ?:
    prerelease <=> other.prerelease
  }

  @Override boolean equals(Object other) {
    other in SemanticVersion &&
      compareTo(other) == 0 &&
      build == other.build
  }
}
