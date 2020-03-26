package com.e4d.build

/**
 * A text value that can interpreted from its qualifier
 * depending on a resolver.
 */
class TextValue {
  final String qualifier
  TextValueResolver resolver

  /**
   * Constructor
   *
   * @param qualifier A string that denotes the value.
   * @param options A map of arbitrary options.
   */
  TextValue(Map options=[:], String qualifier) {
    this.qualifier = qualifier
    resolver = options.resolver
  }

  /**
   * Constructor
   */
  TextValue() {
  }

  /**
   * Resolves the value from its qualifier using specified resolver.
   *
   * @param resolver A resolver that will be used to resolve the value.
   *                 It overrides the resolver bound with the value.
   * @return String contains resolved value.
   */
  String resolve(resolver) {
    resolver = resolver ?: this.resolver
    if (resolver) {
      resolver.resolve(qualifier)
    } else {
      qualifier
    }
  }

  @Override
  String toString() {
    resolve()
  }

  /**
   * Compares the value with other objects.
   * It compares only value's qualifier.
   */
  boolean equals(other) {
    if (other instanceof TextValue) {
      qualifier == other.qualifier
    } else {
      qualifier == other
    }
  }
}
