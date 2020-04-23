package com.e4d.declaration

class Declaration {
  void define(Declaration declaration, Closure definition) {
    definition.delegate = declaration
    definition.resolveStrategy = Closure.DELEGATE_FIRST
    definition.call()
  }

  void define(Closure declaration) {
    define(this, declaration)
  }
}
