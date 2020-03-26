package com.e4d.build

/**
 * Defines interface for a text value resolver.
 */
interface TextValueResolver {
  /**
   * Resolvest a text value from its qualifier.
   *
   * @param qualifier A string that denotes the value.
   * @return String that represents resolved value.
   */
  String resolve(String qualifier)
}
