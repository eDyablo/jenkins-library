package com.e4d.build

class ListUtils {
  static def findMax(def list, def selector = {it}, def comparer = {f, s -> f <=> s}) {
    def maxElement = null
    if (list) {
      def size = list.size()
      if (size > 0) {
        maxElement = list[0]
        def maxValue = selector(maxElement)
        for (def i = 1; i < size; i++) {
          def element = list[i]
          def value = selector(element)
          if (comparer(value, maxValue) == 1) {
            maxElement = element
            maxValue = value
          }
        }
      }
    }
    return maxElement
  }
  
  static def findMin(def list, def selector = {it}, def comparer = {f, s -> s <=> f}) {
    return findMax(list, selector, comparer)
  }
}
