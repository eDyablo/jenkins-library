package com.e4d.service

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class ServiceInstanceReferenceTest {
  @Test void constructed_from_null_text_has_null_name() {
    assertThat(ServiceInstanceReference.fromText(), allOf(
      hasProperty('name', equalTo(null)),
    ))
  }

  @Test void constructed_from_empty_text_has_null_name() {
    assertThat(ServiceInstanceReference.fromText(''), allOf(
      hasProperty('name', equalTo(null)),
    ))
  }

  @Test void constructed_from_whitespaces_has_null_name() {
    assertThat(ServiceInstanceReference.fromText('  '), allOf(
      hasProperty('name', equalTo(null)),
    ))
  }

    @Test void constructed_from_one_word_has_name_equal_to_the_word() {
    assertThat(ServiceInstanceReference.fromText('word'), allOf(
      hasProperty('name', equalTo('word')),
    ))
  }

  @Test void constructed_from_two_words_separated_by_column_has_name_equal_to_first_word() {
    assertThat(ServiceInstanceReference.fromText('first:second'), allOf(
      hasProperty('name', equalTo('first')),
    ))
  }

  @Test void constructed_from_two_words_separated_by_column_and_whitespaces_has_name_equal_to_first_word() {
    assertThat(ServiceInstanceReference.fromText(' first : second '), allOf(
      hasProperty('name', equalTo('first')),
    ))
  }
}
