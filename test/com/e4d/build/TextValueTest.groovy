package com.e4d.build

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*

class TextValueTest {
  final TextValue value = new TextValue('qualifier')

  @Test void newly_created_has_null_qualifier_when_it_is_not_specified() {
    def value = new TextValue()
    assertThat(value.qualifier, is(null))
  }

  @Test void newly_created_has_specified_qualifier() {
    assertThat(value.qualifier, is('qualifier'))
  }

  @Test void resolve_returns_qualificator_when_no_resolver_specified() {
    assertThat(value.resolve(), is('qualifier'))
  }

  @Test void resolve_returns_resolved_qualificator_when_resolver_is_specified() {
    def resolver = mock(TextValueResolver.class)
    when(resolver.resolve('qualifier')).thenReturn('resolved')
    assertThat(value.resolve(resolver), is('resolved'))
  }

  @Test void resolve_uses_internal_resolver_when_no_resolver_specified() {
    value.resolver = mock(TextValueResolver.class)
    when(value.resolver.resolve('qualifier')).thenReturn('resolved')
    assertThat(value.resolve(), is('resolved'))
  }

  @Test void toString_resolves_qualifier() {
    assertThat(value.toString(), is('qualifier'))
  }

  @Test void equals_does_not_take_resolver_into_account() {
    def resolver = mock(TextValueResolver.class)
    def first = new TextValue('qualifier', resolver: resolver)
    assertThat(first, allOf(
      equalTo(new TextValue('qualifier')),
      not(equalTo(new TextValue('q', resolver: resolver)))
    ))
  }

  @Test void equals_can_compare_with_string() {
    assertThat(value, allOf(
      equalTo('qualifier'),
      not(equalTo('q')),
    ))
  }
}
