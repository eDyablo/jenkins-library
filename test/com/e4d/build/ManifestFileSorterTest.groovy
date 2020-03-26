package com.e4d.build

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class ManifestFileSorterTest {
  final def sorter = new ManifestFileSorter()

  @Test void triage_returns_empty_list_when_no_paths_specified() {
    assertThat(sorter.triage(), is(empty()))
  }

  @Test void triage_returns_empty_list_when_paths_is_an_empty_list() {
    assertThat(sorter.triage([]), is(empty()))
  }

  @Test void triage_returns_one_element_with_no_namespace_contains_one_path_when_one_path_specified() {
    assertThat(
      sorter.triage('single'),
      contains(allOf(
        hasProperty('paths', contains('single')),
        hasProperty('namespace', emptyString())
      ))
    )
  }

  @Test void triage_returns_one_element_with_namespace_equal_to_parent_path() {
    assertThat(
      sorter.triage('parent/single'),
      contains(hasProperty('namespace', equalTo('parent')))
    )
    assertThat(
      sorter.triage('grand/parent/single'),
      contains(hasProperty('namespace', equalTo('grand-parent')))
    )
  }

  @Test void triage_returns_two_elements_with_different_namspaces_for_two_paths_with_different_parent_paths() {
    assertThat(
      sorter.triage('first/element', 'second/element'),
      contains(
        hasProperty('namespace', equalTo('first')),
        hasProperty('namespace', equalTo('second'))
      )
    )
  }

  @Test void triage_remove_prefix_from_namespace_and_preserves_path() {
    assertThat(
      sorter.triage('prefix/element', prefix: 'prefix'),
      contains(allOf(
        hasProperty('paths', contains('prefix/element')),
        hasProperty('namespace', emptyString())
      ))
    )
    assertThat(
      sorter.triage('prefix/parent/element', prefix: 'prefix'),
      contains(allOf(
        hasProperty('paths', contains('prefix/parent/element')),
        hasProperty('namespace', equalTo('parent'))
      ))
    )
    assertThat(
      sorter.triage('prefix/grand/parent/element', prefix: 'prefix'),
      contains(allOf(
        hasProperty('paths', contains('prefix/grand/parent/element')),
        hasProperty('namespace', equalTo('grand-parent'))
      ))
    )
    assertThat(
      sorter.triage('multi/level/prefix/grand/parent/element',
        prefix: 'multi/level/prefix'),
      contains(allOf(
        hasProperty('paths', contains('multi/level/prefix/grand/parent/element')),
        hasProperty('namespace', equalTo('grand-parent'))
      ))
    )
  }

  @Test void triage_removes_all_prefixes_from_namespaces() {
    assertThat(
      sorter.triage('first/element', 'second/element',
        prefixes: ['first', 'second']),
      everyItem(hasProperty('namespace', emptyString()))
    )
  }

  @Test void triage_removes_prefixes_that_are_substrings_of_each_other() {
    assertThat(
      sorter.triage(
        'first/element', 'first/second/element', 'first/second/third/element',
        prefixes: ['first', 'first/second', 'first/second/third']),
      everyItem(hasProperty('namespace', emptyString()))
    )
  }
}
