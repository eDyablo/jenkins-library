package com.e4d.build

import com.e4d.build.SemanticVersion
import com.e4d.dotnet.DotnetTool
import com.e4d.nuget.NugetRepository
import com.e4d.pipeline.DummyPipeline

import org.junit.*
import org.mockito.ArgumentCaptor
import static com.e4d.mockito.Matchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class CSProjectNugetPublisherTest extends CSProjectNugetPublisher {
  final DotnetTool dotnet = mock(DotnetTool)
  final static emptyContext = [:]

  @Override def createDotnetTool(context) {
    dotnet
  }

  @Test void deploy_packs_specified_csharp_project() {
    project = 'project'
    deploy(emptyContext)
    verify(dotnet).csprojPack(any(Map), eq('project'))
  }

  @Test void deploy_packs_csproj_with_specified_version() {
    version = 'version'
    deploy(emptyContext)
    verify(dotnet).csprojPack(mapContains(version: 'version'), anyObject())
  }

  @Test void deploy_packs_csproj_with_specified_version_prefix() {
    versionPrefix = 'prefix'
    deploy(emptyContext)
    verify(dotnet).csprojPack(mapContains(versionPrefix: 'prefix'), anyObject())
  }

  @Test void deploy_packs_csproj_with_specified_version_suffix() {
    versionSuffix = 'suffix'
    deploy(emptyContext)
    verify(dotnet).csprojPack(mapContains(versionSuffix: 'suffix'), anyObject())
  }

  @Test void deploy_pushes_packed_nuget_to_specified_server() {
    // Arrange
    server = 'server'
    doReturn([package: 'package']).when(dotnet).csprojPack(any(), eq(project))
    // Act
    deploy(emptyContext)
    // Assert
    verify(dotnet).nugetPush(mapContains(source: 'server'), anyObject())
  }

  @Test void deploy_pushes_nuget_using_nuget_server_from_context_when_no_server_specified() {
    // Arrange
    server = null
    doReturn([package: 'package']).when(dotnet).csprojPack(any(), eq(project))
    // Act
    deploy([nuget: [server: 'server from context']])
    // Assert
    verify(dotnet).nugetPush(mapContains(source: 'server from context'), anyObject())
  }

  @Test void deploy_pushes_nuget_preffering_specified_server_over_nuget_server_from_context() {
    // Arrange  
    server = 'server'
    doReturn([package: 'package']).when(dotnet).csprojPack(any(), eq(project))
    // Act
    deploy([nuget: [server: 'server from context']])
    // Assert
    verify(dotnet).nugetPush(mapContains(source: 'server'), anyObject())
  }

  @Test void deploy_pushes_nuget_with_specified_api_key() {
    // Arrange
    apiKey = 'key'
    doReturn([package: 'package']).when(dotnet).csprojPack(any(), eq(project))
    // Act
    deploy(emptyContext)
    // Assert
    verify(dotnet).nugetPush(mapContains(api_key: 'key'), anyObject())
  }

  @Test void deploy_pushes_nuget_using_nuget_api_key_from_context_when_no_api_key_specified() {
    // Arrange
    apiKey = null
    doReturn([package: 'package']).when(dotnet).csprojPack(any(), eq(project))
    // Act
    deploy([nuget: [api_key: 'key from context']])
    // Assert
    verify(dotnet).nugetPush(mapContains(api_key: 'key from context'), anyObject())
  }

  @Test void deploy_pushes_nuget_preffering_specified_api_key_over_nuget_api_key_from_context() {
    // Arrange
    apiKey = 'key'
    doReturn([package: 'package']).when(dotnet).csprojPack(any(), eq(project))
    // Act
    deploy([nuget: [api_key: 'key from context']])
    // Assert
    verify(dotnet).nugetPush(mapContains(api_key: 'key'), eq('package'))
  }

  @Test void can_deploy_when_no_changed_file_paths_in_context() {
    final tests = [
      emptyContext,
      [changedFiles: null],
      [changedFiles: []],
    ]
    tests.each { context ->
      assertThat("\n     For: ${ context }", canDeploy(context), is(true))
    }
  }

  @Test void can_deploy_when_any_changed_file_path_in_context_contains_specified_project() {
    project = 'project'
    assertThat(canDeploy(changedFiles: [
      'file',
      'dir/file',
      'dir/the project/file',
    ]), is(true))
  }

  @Test void can_deploy_even_when_none_changed_file_path_in_context_contains_specified_project() {
    project = 'project'
    assertThat(canDeploy(changedFiles: [
      'file',
      'dir/file',
    ]), is(true))
  }

  @Test void uses_artifact_version_from_context_when_the_publisher_has_no_version_specified() {
    // Arrange
    version = null
    final context = [artifactVersion: new SemanticVersion(
      major: 1, minor: 2, patch: 3)]
    // Act
    deploy(context)
    // Assert
    final mapArg = ArgumentCaptor.forClass(Map)
    verify(dotnet).csprojPack(mapArg.capture(), eq(project))
    assertThat(mapArg.value, hasEntry('version', '1.2.3'))
  }

  @Test void converts_artifact_version_from_context_into_semver_1_0_compliant() {
    // Arrange
    version = null
    final context = [
      artifactVersion: new SemanticVersion(
        major: 1, minor: 2, patch: 3,
        prerelease: ['r1', 'r2'], build: ['b1', 'b2'])
    ]
    // Act
    deploy(context)
    // Assert
    final mapArg = ArgumentCaptor.forClass(Map)
    verify(dotnet).csprojPack(mapArg.capture(), eq(project))
    assertThat(mapArg.value, hasEntry('version', '1.2.3-r1.r2.b1.b2'))
  }

  @Test void packs_with_zero_version_when_neiser_version_nor_context_artifact_version_is_set() {
    // Arrange
    version = null
    final context = [:]
    // Act
    deploy(context)
    // Assert
    final mapArg = ArgumentCaptor.forClass(Map)
    verify(dotnet).csprojPack(mapArg.capture(), any())
    assertThat(mapArg.value, hasEntry('version', '0.0.0'))
  }

  @Test void deploy_does_not_publish_artifact_existing_in_the_nuget_repository() {
    // Arrange
    project = 'existing'
    version = 'existing'
    final context = [nugetRepository: mock(NugetRepository)]
    doReturn(true).when(context.nugetRepository).hasNuget(mapContains(
      name: 'existing', version: 'existing'))
    // Act
    deploy(context)
    // Verify
    verify(dotnet, never()).nugetPush(any(), eq('existing'))
  }

  @Test void deploy_publishes_new_artifact_not_existing_in_the_nuget_repository() {
    // Arrange
    project = 'new project'
    version = 'new version'
    final context = [nugetRepository: mock(NugetRepository)]
    doReturn(false).when(context.nugetRepository).hasNuget(mapContains(
      name: 'new project', version: 'new version'))
    doReturn([package: 'new package']).when(dotnet).csprojPack(any(), eq('new project'))
    // Act
    deploy(context)
    // Verify
    verify(dotnet).nugetPush(any(), eq('new package'))
  }

  @Test void deploy_warns_when_package_for_the_project_exists_in_the_repository() {
    // Arrange
    project = 'project'
    version = 'version'
    final context = [
      nugetRepository: mock(NugetRepository),
      pipeline: mock(DummyPipeline),
    ]
    doReturn([package: 'package', version: 'version']).when(dotnet).csprojPack(any(), any())
    doReturn(true).when(context.nugetRepository).hasNuget(mapContains(
      name: 'project', version: 'version'))
    // Act
    deploy(context)
    // Verify
    verify(context.pipeline).echo(
      'WARNING: Nuget package project:version cannot be published over existing one')
  }

  @Test void artifact_version_is_equal_to_version_from_the_publisher_when_it_is_not_null() {
    version = 'version'
    assertThat(getArtifactVersion(emptyContext), is(equalTo('version')))
  }

  @Test void artifact_version_gets_build_from_artifact_version_in_context_when_no_version_set_for_the_publisher() {
    final context = [artifactVersion: new SemanticVersion(
      major: 1, minor: 2, patch: 3)]
    [null, '', ' ', ' \t  '].each { version ->
      this.version = version
      assertThat("\n     For: '${ version }'",
        getArtifactVersion(context), is(equalTo('1.2.3')))
    }
  }
}
