package com.e4d.pipeline

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*

class PipelineTextValueResolverTest {
  @Test void resolve_returns_null_for_null_qualifier() {
    def resolver = new PipelineTextValueResolver()
    assertThat(resolver.resolve(null), is(null))
  }

  @Test void resolve_returns_empty_string_for_empty_qualifier() {
    def resolver = new PipelineTextValueResolver()
    assertThat(resolver.resolve(''), is(emptyString()))
  }

  @Test void resolve_returns_qualifier_when_it_does_not_start_with_hash_sign() {
    def resolver = new PipelineTextValueResolver()
    assertThat(resolver.resolve('qualifier'), is('qualifier'))
  }

  @Test void resolve_returns_string_from_credentials_when_qualifier_starts_with_hash_sign() {
    def pipeline = [env:[:]]
    pipeline.string = { Map m -> m }
    pipeline.withCredentials = { List creds, Closure code ->
      creds.each {
        if (it.credentialsId == 'qualifier') {
          pipeline.env[it.variable] = 'from credentials'
        }
      }
      code()
    }
    def resolver = new PipelineTextValueResolver(pipeline)
    assertThat(resolver.resolve('#qualifier'), is('from credentials'))
  }
}
