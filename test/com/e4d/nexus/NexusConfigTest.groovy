package com.e4d.nexus

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class NexusConfigTest {
  final config = new NexusConfig()

  @Test void newly_created_has_null_apiKey() {
    assertThat(config.apiKey, is(null))
  }

  @Test void can_set_api_key_as_string() {
    config.apiKey = 'api key'
    assertThat(config.apiKey.toString(), is('api key'))
  }
}
