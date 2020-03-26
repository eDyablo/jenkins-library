package com.e4d.mockito

import org.mockito.ArgumentMatcher
import static org.mockito.ArgumentMatchers.argThat

class MapContainsMatcher implements ArgumentMatcher<Map> {
  final Map reference

  MapContainsMatcher(Map reference) {
    this.reference = reference
  }

  @Override boolean matches(Map map) {
    reference.every { k, v ->
      map[k] == v
    }
  }

  @Override String toString() {
    def image = reference.collect { k, v ->
      "\"${ k }\" = \"${ v }\""
    }.join(', ')
    "<map contains {${ image }}>"
  }
}

class Matchers {
  static Map mapThat(ArgumentMatcher<Map> matcher) {
    argThat(matcher)
  }

  static Map mapContains(Map reference) {
    mapThat(new MapContainsMatcher(reference))
  }
}
