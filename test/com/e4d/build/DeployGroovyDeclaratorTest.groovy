package com.e4d.build

import org.junit.*
import org.junit.runner.*
import org.mockito.junit.MockitoJUnitRunner
import static org.junit.Assert.*
import static org.mockito.Mockito.*
import static org.mockito.Matchers.*

class DeployGroovyDeclaratorTest {
  DeployDeclaration declaration
  DeployGroovyDeclarator declarator

  @Before void beforeEach() {
    declaration = mock(DeployDeclaration.class)
    declarator = new DeployGroovyDeclarator(declaration)
  }

  @Test void declare_from_empty_text() {
    declarator.declare('')
  }

  @Test void declare_from_text_contains_publish_calls_publish_once() {
    declarator.declare('''
      publish {
      }
    ''')
    verify(declaration, times(1)).publish(any(Closure.class))
  }
}
