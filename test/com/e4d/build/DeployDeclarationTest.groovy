package com.e4d.build

import org.junit.*
import static org.mockito.Mockito.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DeployDeclarationTest {
  final DeployDeclaration deploy = new DeployDeclaration()

  @Test void publish_gets_delegated_to_publish_declaration() {
    def delegated = null
    deploy.publish {
      delegated = delegate
    }
    assertThat(delegated, is(instanceOf(PublishDeclaration.class)))
  }
}
