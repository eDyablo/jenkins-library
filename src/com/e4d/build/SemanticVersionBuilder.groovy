package com.e4d.build

import com.cloudbees.groovy.cps.NonCPS

class SemanticVersionBuilder {
  final data = [
    major:      0,
    minor:      0,
    patch:      0,
    build:      new ArrayList<String>(),
    prerelease: new ArrayList<String>(),
  ]

  @NonCPS
  SemanticVersion build() {
    new SemanticVersion(data)
  }

  @NonCPS
  SemanticVersionBuilder major(short value) {
    data.major = value
    return this
  }

  @NonCPS
  SemanticVersionBuilder major(int value) {
    major(value as short)
  }

  @NonCPS
  SemanticVersionBuilder minor(short value) {
    data.minor = value
    return this
  }

  @NonCPS
  SemanticVersionBuilder minor(int value) {
    minor(value as short)
  }

  @NonCPS
  SemanticVersionBuilder patch(short value) {
    data.patch = value
    return this
  }

  @NonCPS
  SemanticVersionBuilder patch(int value) {
    patch(value as short)
  }

  @NonCPS
  SemanticVersionBuilder build(String value) {
    if (value) {
      data.build << value.trim()
    }
    return this
  }

  @NonCPS
  SemanticVersionBuilder build(List<String> value) {
    if (value) {
      data.build += value
    }
    return this
  }

  @NonCPS
  SemanticVersionBuilder build(int value) {
    build(value as String)
  }

  @NonCPS
  SemanticVersionBuilder build(Object[] value) {
    build(value.collect { it as String })
  }

  @NonCPS
  SemanticVersionBuilder prerelease(String value) {
    if (value) {
      data.prerelease << value.trim()
    }
    return this
  }

  @NonCPS
  SemanticVersionBuilder prerelease(List<String> value) {
    if (value) {
      data.prerelease += value
    }
    return this
  }

  @NonCPS
  SemanticVersionBuilder prerelease(int value) {
    prerelease(value as String)
  }

  @NonCPS
  SemanticVersionBuilder prerelease(Object[] value) {
    prerelease(value.collect { it as String })
  }

  @NonCPS
  SemanticVersionBuilder resetBuild() {
    data.build.clear()
    return this
  }

  @NonCPS
  SemanticVersionBuilder resetPrerelease() {
    data.prerelease.clear()
    return this
  }

  @NonCPS
  SemanticVersionBuilder fromGitTag(String tag) {
    resetBuild()
    resetPrerelease()
    if (tag) {
      final int maxCoreNumberSize = 6
      final tokens = tag.minus('v').tokenize('-')
      int end = tokens.size() - 1
      if (tokens[end].startsWith('g')) {
        build('git')
        build('h' + tokens[end] - 'g')
        end -= 2
      }
      final corePart = tokens[0]
      if (corePart.size() > maxCoreNumberSize &&
          corePart.chars.every { it != '.' }) {
        build('git')
        build('h' + corePart)
      } else {
        core(corePart)
      }
      for (int i = 1; i <= end; i++) {
        prerelease(tokens[i])
      }
    }
    return this
  }

  @NonCPS
  SemanticVersionBuilder core(String value) {
    final parts = value?.trim()?.tokenize('.') ?: []
    core(parts as int[])
  }

  @NonCPS
  SemanticVersionBuilder core(int[] values) {
    final int size = values.size()
    if (size > 0) { major(values[0]) }
    if (size > 1) { minor(values[1]) }
    if (size > 2) { patch(values[2]) }
    for (int i = 3; i < size; ++i) { build(values[i]) }
    return this
  }
}
