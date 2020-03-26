package com.e4d.nexus

import com.e4d.nexus.NexusClient
import com.e4d.nexus.NexusComponent
import org.junit.*
import org.mockito.*
import static com.e4d.mockito.Matchers.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*

class NexusNugetRepositoryTest {
  final client = mock(NexusClient)
  final repository = new NexusNugetRepository(
    client: client, repository: 'repository')
  
  @Test void searches_nuget_repository_for_name_and_version_when_hasNuget_called_with_name_and_version() {
    repository.hasNuget('name:version')
    verify(client).searchComponents(mapContains(
      repository: 'repository',
      name: 'name',
      version: 'version',
    ))
  }

  @Test void has_no_nuget_when_client_did_not_find_it() {
    when(client.searchComponents(any())).thenReturn([] as NexusComponent[])
    assertThat(repository.hasNuget('name'), is(false))
  }

  @Test void has_nuget_when_client_found_it() {
    when(client.searchComponents(any())).thenReturn([new NexusComponent()] as NexusComponent[])
    assertThat(repository.hasNuget('name'), is(true))
  }
}

class NexusNugetRepositoryHasNugetTest {
  final repository = mock(NexusNugetRepository)

  @Before void beforeEachTest() {
    when(repository.hasNuget(anyString())).thenCallRealMethod()
  }

  @Test void uses_name_when_tag_contains_only_name() {
    repository.hasNuget('name')
    verify(repository).hasNuget(mapContains(name: 'name'))
  }

  @Test void uses_name_and_version_when_tag_contains_name_and_version() {
    repository.hasNuget('name:version')
    verify(repository).hasNuget(mapContains(name: 'name', version: 'version'))
  }
}
