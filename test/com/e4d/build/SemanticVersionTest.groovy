package com.e4d.build

import org.junit.*
import org.junit.rules.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class SemanticVersionTest {
  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Test void has_initial_values_when_newly_created() {
    final def version = new SemanticVersion()
    assertThat(version, allOf(
      hasProperty('minor', is(0 as short)),
      hasProperty('major', is(0 as short)),
      hasProperty('patch', is(0 as short)),
      hasProperty('build', is('')),
      hasProperty('prerelease', is('')),
    ))
  }

  @Test void can_not_be_created_with_negative_major() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Major version component is negative')
    final def version = new SemanticVersion(major: -1)
  }

  @Test void can_not_be_created_with_negative_minor() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Minor version component is negative')
    final def version = new SemanticVersion(minor: -1)
  }

  @Test void can_not_be_created_with_negative_patch() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Patch version component is negative')
    final def version = new SemanticVersion(patch: -1)
  }

  @Test void has_core_constructed_from_major_minor_and_patch() {
    final def version = new SemanticVersion(
      major: 1, minor: 2, patch: 3)
    assertThat(version.core, is('1.2.3'))
  }

  @Test void has_textual_presentation_of_core_contains_build_metadata_when_build_is_specified() {
    final def version = new SemanticVersion(
      major: 1, minor: 2, patch: 3, build: 'build')
    assertThat(version.toString(), equalTo('1.2.3+build'))
  }

  @Test void has_textual_presentation_contains_core_when_only_core_components_are_specified() {
    final def version = new SemanticVersion(
      major: 1, minor: 2, patch: 3)
    assertThat(version.toString(), equalTo('1.2.3'))
  }

  @Test void has_textual_presentation_contains_core_with_pre_release_metadata_when_pre_release_is_specified() {
    final def version = new SemanticVersion(
      major: 1, minor: 2, patch: 3, prerelease: 'prerelease')
    assertThat(version.toString(), equalTo('1.2.3-prerelease'))
  }

  @Test void has_textual_presentation_contains_core_pre_release_and_build_metadata_when_all_are_specified() {
    final def version = new SemanticVersion(
      major: 1, minor: 2, patch: 3, build: 'build', prerelease: 'prerelease')
    assertThat(version.toString(), equalTo('1.2.3-prerelease+build'))
  }

  @Test void treats_whitespaces_in_build_as_empty_build() {
    final def version = new SemanticVersion(build: ' \t  ')
    assertThat(version.build, is(''))
    assertThat(version.buildIds, is(empty()))
  }

  @Test void treats_whitespaces_in_pre_release_as_empty_pre_release() {
    final def version = new SemanticVersion(prerelease: ' \t  ')
    assertThat(version.prerelease, is(''))
    assertThat(version.prereleaseIds, is(empty()))
  }

  @Test void has_release_constructed_from_core_and_pre_release() {
    final def version = new SemanticVersion(
      major: 1, minor: 2, patch: 3, prerelease: 'prerelease')
    assertThat(version.release, is('1.2.3-prerelease'))
  }

  @Test void compares_major_components() {
    final def first = new SemanticVersion(major: 1)
    final def second = new SemanticVersion(major: 2)
    assertThat(first.compareTo(second), is(-1))
    assertThat(second.compareTo(first), is(1))
    assertThat(first.compareTo(first), is(0))
  }

  @Test void compares_minor_components_when_major_are_equal() {
    final def first = new SemanticVersion(major: 1, minor: 1)
    final def second = new SemanticVersion(major: 1, minor: 2)
    assertThat(first.compareTo(second), is(-1))
    assertThat(second.compareTo(first), is(1))
    assertThat(first.compareTo(first), is(0))
  }

  @Test void compares_patch_components_when_major_and_minor_are_equal() {
    final def first = new SemanticVersion(major: 1, minor: 1, patch: 1)
    final def second = new SemanticVersion(major: 1, minor: 1, patch: 2)
    assertThat(first.compareTo(second), is(-1))
    assertThat(second.compareTo(first), is(1))
    assertThat(first.compareTo(first), is(0))
  }

  @Test void when_compares_does_not_take_into_account_the_build() {
    final def first = new SemanticVersion(major: 1, build: '1')
    final def second = new SemanticVersion(major: 1, build: '2')
    assertThat(first.compareTo(second), is(0))
    assertThat(second.compareTo(first), is(0))
  }

  @Test void compares_pre_releases_when_core_components_are_equal() {
    final def first = new SemanticVersion(prerelease: 'a')
    final def second = new SemanticVersion(prerelease: 'b')
    assertThat(first.compareTo(second), is(-1))
    assertThat(second.compareTo(first), is(1))
  }

  @Test void has_one_prerelease_id_when_single_word_set_for_prerelease() {
    final def version = new SemanticVersion(prerelease: 'word')
    assertThat(version.prereleaseIds, equalTo(['word']))
  }

  @Test void has_all_prerelease_ids_specified_during_construction() {
    final def version = new SemanticVersion(prerelease: ['first', 'second'])
    assertThat(version.prereleaseIds, equalTo(['first', 'second']))
  }

  @Test void has_empty_prerelease_ids_when_specified_prerelease_is_null() {
    final def version = new SemanticVersion(prerelease: null)
    assertThat(version.prereleaseIds, is(empty()))
  }

  @Test void its_prerelease_is_dot_separated_prerelease_ids() {
    final def version = new SemanticVersion(prerelease: ['first', 'second'])
    assertThat(version.prerelease, is('first.second'))
  }

  @Test void has_one_build_id_when_single_word_set_for_build() {
    final def version = new SemanticVersion(build: 'word')
    assertThat(version.buildIds, equalTo(['word']))
  }

  @Test void has_all_build_ids_specified_during_construction() {
    final def version = new SemanticVersion(build: ['first', 'second'])
    assertThat(version.buildIds, equalTo(['first', 'second']))
  }

  @Test void has_empty_build_ids_when_specified_build_is_null() {
    final def version = new SemanticVersion(build: null)
    assertThat(version.buildIds, is(empty()))
  }

  @Test void its_build_is_dot_separated_build_ids() {
    final def version = new SemanticVersion(build: ['first', 'second'])
    assertThat(version.build, is('first.second'))
  }

  @Test void versions_with_different_builds_are_not_equal() {
    final def first = new SemanticVersion(build: '1')
    final def second = new SemanticVersion(build: '2')
    assertThat(first.equals(second), is(false))
    assertThat(second.equals(first), is(false))
  }

  @Test void versions_with_equal_builds_are_not_equal() {
    final def first = new SemanticVersion(build: '1')
    final def second = new SemanticVersion(build: '1')
    assertThat(first.equals(second), is(true))
    assertThat(second.equals(first), is(true))
  }
}
