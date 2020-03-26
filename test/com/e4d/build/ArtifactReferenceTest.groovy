package com.e4d.build

import org.junit.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ArtifactReferenceTest {
  @Test void canCreate() {
    def ref = new ArtifactReference('name', 'tag')
    assertThat(ref, allOf(
      hasProperty('name', equalTo('name')),
      hasProperty('tag', equalTo('tag'))
    ))
  }
}
