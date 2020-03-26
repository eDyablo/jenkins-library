package com.e4d.build

class Path {
  static String combine(String first, String second) {
    return [first, second].join('/')
  }

  static String combine(ArrayList<String> components) {
    return components.join('/')
  }

  static String[] split(String path) {
    return path.tokenize('/')
  }

  static String directory(String path) {
    return path.tokenize('/')[0..-2].join('/')
  }

  static String baseName(String path) {
    return path.tokenize('/')[-1]
  }
}
