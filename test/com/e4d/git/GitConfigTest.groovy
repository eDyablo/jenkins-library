package com.e4d.git

import org.junit.*
import org.mockito.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class GitConfigTest {
  @Test void newly_created_has_valid_default_values() {
    final def git = new GitConfig()
    assertThat(git, allOf(
      hasProperty('protocol', is(null)),
      hasProperty('host', is(null)),
      hasProperty('owner', is(null)),
      hasProperty('repository', is(null)),
      hasProperty('hostURL', is('')),
      hasProperty('ownerURL', is('')),
      hasProperty('repositoryURL', is('')),
      hasProperty('baseUrl', is('')),
    ))
  }

  @Test void has_valid_URLs_when_all_properties_are_set() {
    final def git = new GitConfig()
    git.with {
      protocol = 'protocol'
      host = 'host'
      owner = 'owner'
      repository = 'repository'
    }
    assertThat(git, allOf(
      hasProperty('hostURL', is('protocol://host')),
      hasProperty('ownerURL', is('protocol://host/owner')),
      hasProperty('repositoryURL', is('protocol://host/owner/repository')),
    ))
  }

  @Test void has_host_set_from_baseUrl_when_set_only_host_to_baseUrl() {
    final def git = new GitConfig()
    git.baseUrl = 'host'
    assertThat(git.host, is('host'))
  }

  @Test void does_not_change_protocol_when_set_only_host_to_baseUrl() {
    final def git = new GitConfig()
    git.protocol = 'old-protocol'
    git.baseUrl = 'host'
    assertThat(git.protocol, is('old-protocol'))
  }

  @Test void has_protocol_set_from_baseUrl_when_protocol_and_host_are_specified_for_baseUrl() {
    final def git = new GitConfig()
    git.protocol = 'old-protocol'
    git.baseUrl = 'new-protocol://host'
    assertThat(git.protocol, is('new-protocol'))
  }

  @Test void has_protocol_set_from_baseUrl_when_only_protocol_is_specified_for_baseUrl() {
    final def git = new GitConfig()
    git.protocol = 'old-protocol'
    git.baseUrl = 'new-protocol://'
    assertThat(git.protocol, is('new-protocol'))
  }

  @Test void has_null_host_when_only_protocol_is_specified_for_baseUrl() {
    final def git = new GitConfig()
    git.host = 'host'
    git.baseUrl = 'protocol://'
    assertThat(git.host, is(null))
  }

  @Test void has_null_owner_when_only_host_is_specified_for_baseUrl() {
    final def git = new GitConfig()
    git.owner = 'owner'
    git.baseUrl = 'host'
    assertThat(git.owner, is(null))
  }

  @Test void has_protocol_from_baseUrl_when_host_and_owner_are_specified_for_baseUrl() {
    final def git = new GitConfig()
    git.owner = 'old-owner'
    git.baseUrl = 'host/new-owner'
    assertThat(git.owner, is('new-owner'))
  }

  @Test void has_protocol_from_baseUrl_when_protocol_host_and_owner_are_specified_for_baseUrl() {
    final def git = new GitConfig()
    git.owner = 'old-owner'
    git.baseUrl = 'protocol://host/new-owner'
    assertThat(git.owner, is('new-owner'))
  }

  @Test void has_null_repository_when_only_protocol_is_specified_for_baseUrl() {
    final def git = new GitConfig()
    git.repository = 'repository'
    git.baseUrl = 'protocol://'
    assertThat(git.repository, is(null))
  }

  @Test void has_null_repository_when_only_host_is_specified_for_baseUrl() {
    final def git = new GitConfig()
    git.repository = 'repository'
    git.baseUrl = 'host'
    assertThat(git.repository, is(null))
  }

  @Test void has_repository_set_from_baseUrl_when_host_and_owner_and_repository_are_specified_for_baseUrl() {
    final def git = new GitConfig()
    git.repository = 'old-repository'
    git.baseUrl = 'host/owner/new-repository'
    assertThat(git.repository, is('new-repository'))
  }

  @Test void has_baseUrl_constructed_from_protocol_only_when_only_protocol_is_set() {
    final def git = new GitConfig()
    git.protocol = 'protocol'
    assertThat(git.baseUrl, is('protocol://'))
  }

  @Test void has_baseUrl_constructed_from_owner_only_when_only_host_is_set() {
    final def git = new GitConfig()
    git.host = 'host'
    assertThat(git.baseUrl, is('host'))
  }

  @Test void has_baseUrl_constructed_from_protocol_and_host_when_only_they_are_set() {
    final def git = new GitConfig()
    git.with {
      protocol = 'protocol'
      host = 'host'
    }
    assertThat(git.baseUrl, is('protocol://host'))
  }

  @Test void has_baseUrl_constructed_from_protocol_host_and_owner_when_only_they_are_set() {
    final def git = new GitConfig()
    git.with {
      protocol = 'protocol'
      host = 'host'
      owner = 'owner'
    }
    assertThat(git.baseUrl, is('protocol://host/owner'))
  }

  @Test void has_baseUrl_constructed_from_host_and_owner_when_only_they_are_set() {
    final def git = new GitConfig()
    git.with {
      host = 'host'
      owner = 'owner'
    }
    assertThat(git.baseUrl, is('host/owner'))
  }

  @Test void has_host_set_from_baseUrl_when_protocol_host_and_owner_are_specified_for_baseUrl() {
    final def git = new GitConfig()
    git.baseUrl = 'protocol://host/owner'
    assertThat(git.host, is('host'))
  }
}
