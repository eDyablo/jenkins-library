package com.e4d.job

class JobDeclaration {
  def script

  JobDeclaration(def script) {
    this.script = script
  }

  def declare(def declaration, def closure) {
    closure.delegate = declaration
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure()
  }
}
