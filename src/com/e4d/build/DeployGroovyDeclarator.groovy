package com.e4d.build

import org.codehaus.groovy.control.CompilerConfiguration
import com.cloudbees.groovy.cps.NonCPS
import groovy.util.DelegatingScript

/**
 * Interpreter of deploy script.
 */
class DeployGroovyDeclarator {
  final DeployDeclaration declaration

  /**
   * Constructor.
   */
  DeployGroovyDeclarator(DeployDeclaration declaration) {
    this.declaration = declaration
  }

  /**
   * Reads and runs script from the text.
   *
   * @param text A text containing script.
   */
  @NonCPS
  def declare(String text) {
    def script = parse(text)
    script.setDelegate(declaration)
    script.run()
  }

  @NonCPS
  private def parse(String text) {
    def groovyConfig = new CompilerConfiguration()
    groovyConfig.scriptBaseClass = DelegatingScript.class.name
    def groovy = new GroovyShell(this.class.classLoader, new Binding(), groovyConfig)
    (DelegatingScript)groovy.parse(text)
  }
}
