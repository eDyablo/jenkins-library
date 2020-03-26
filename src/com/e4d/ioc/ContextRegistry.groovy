package com.e4d.ioc

import com.cloudbees.groovy.cps.NonCPS

class ContextRegistry implements Serializable {
  private static Context _context

  @NonCPS
  static void registerContext(Context newContext) {
    _context = newContext
  }

  @NonCPS
  static void registerDefaultContext(pipeline) {
    _context = new DefaultContext(pipeline)
  }

  @NonCPS
  static Context getContext() {
    _context
  }
}
