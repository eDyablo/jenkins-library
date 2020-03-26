package com.e4d.build

import com.cloudbees.groovy.cps.NonCPS

class VersionTag implements Comparable<VersionTag> {
  Integer[] tokens

  static VersionTag fromText(String text) {
    Integer[] tokens = text?.tokenize('.')
        .collect { Integer.parseInt(it) } ?: []
    return new VersionTag(tokens: tokens)
  }

  static VersionTag minimal() {
    return VersionTag.fromText('')
  }

  @Override
  @NonCPS
  int compareTo(VersionTag other) {
    compareTokens(this.tokens, other.tokens)
  }

  @NonCPS
  int compareTokens(Integer[] first, Integer[] second) {
    def firstSize = first.size()
    def secondSize = second.size()
    def size = Math.max(firstSize, secondSize)
    def difference = 0
    for (def index = 0; difference == 0 && index < size; index++) {
      def tokenFromFirst = index < firstSize ? first[index] : 0
      def tokenFromSecond = index < secondSize ? second[index] : 0
      difference = tokenFromFirst <=> tokenFromSecond
    }
    return difference
  }

  @Override
  @NonCPS
  String toString() {
    return tokens.join('.')
  }

  @NonCPS
  boolean equals(VersionTag other) {
    return compareTo(other) == 0
  }

  def append(int token) {
    return new VersionTag(tokens: tokens + token)
  }
}
