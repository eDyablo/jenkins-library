package com.e4d.k8s

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class K8sEnvReferenceTest {
  @Test void constructed_from_null_text_has_default_properties() {
    assertThat(K8sEnvReference.fromText(), allOf(
      hasProperty('context', equalTo('default')),
      hasProperty('namespace', equalTo('default')),
    ))
  }

  @Test void constructed_from_empty_text_has_default_properties() {
    assertThat(K8sEnvReference.fromText(''), allOf(
      hasProperty('context', equalTo('default')),
      hasProperty('namespace', equalTo('default')),
    ))
  }

  @Test void constructed_from_whitespaces_has_default_properties() {
    assertThat(K8sEnvReference.fromText('  '), allOf(
      hasProperty('context', equalTo('default')),
      hasProperty('namespace', equalTo('default')),
    ))
  }

  @Test void constructed_from_one_word_has_context_equal_to_the_word_and_default_namespace() {
    assertThat(K8sEnvReference.fromText('word'), allOf(
      hasProperty('context', equalTo('word')),
      hasProperty('namespace', equalTo('default')),
    ))
  }

  @Test void constructed_from_two_words_separated_by_column_has_proper_context_and_namespace() {
    assertThat(K8sEnvReference.fromText('context:namespace'), allOf(
      hasProperty('context', equalTo('context')),
      hasProperty('namespace', equalTo('namespace')),
    ))
  }

  @Test void constructed_from_two_words_separated_by_column_and_whitespaces_has_proper_context_and_namespace() {
    assertThat(K8sEnvReference.fromText(' context : namespace '), allOf(
      hasProperty('context', equalTo('context')),
      hasProperty('namespace', equalTo('namespace')),
    ))
  }
}
