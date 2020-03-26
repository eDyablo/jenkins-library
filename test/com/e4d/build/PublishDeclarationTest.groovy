package com.e4d.build

import org.junit.*
import static org.mockito.Mockito.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class PublishDeclarationTest {
  final def publish = new PublishDeclaration()

  @Test void nuget_gets_delegated_to_publish_nuget_declaration() {
    def delegated = null
    publish.nuget {
      delegated = delegate
    }
    assertThat(delegated, is(instanceOf(PublishNugetDeclaration.class)))
  }
}
