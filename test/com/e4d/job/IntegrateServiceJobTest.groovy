package com.e4d.job

import com.e4d.build.SemanticVersion
import com.e4d.build.SemanticVersionBuilder
import com.e4d.build.TextValue
import com.e4d.curl.CurlTool
import com.e4d.nuget.NugetConfig
import com.e4d.pip.PipConfig
import com.e4d.pipeline.DummyPipeline
import com.e4d.tar.TarTool
import org.junit.*
import org.junit.rules.*
import org.mockito.ArgumentCaptor
import static com.e4d.mockito.Matchers.*
import static org.hamcrest.core.Every.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.argThat
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class IntegrateServiceJobNewlyCreatedTest {
  final pipeline = mock(DummyPipeline)
  final job = new IntegrateServiceJob(pipeline)

  @Test void has_default_git_config() {
    final git = [:]
    git.with(DefaultValues.git)
    assertThat(job.gitConfig, allOf(
      hasProperty('baseUrl', equalTo(git.baseUrl)),
    ))
  }

  @Test void has_default_nexus_config() {
    final nexus = [:]
    nexus.with(DefaultValues.nexus)
    assertThat(job.nexusConfig, allOf(
      hasProperty('baseUrl', equalTo(nexus.baseUrl)),
      hasProperty('port', equalTo(nexus.port)),
      hasProperty('credsId', equalTo(nexus.credsId)),
      hasProperty('apiKey', equalTo(nexus.apiKey)),
    ))
  }

  @Test void has_default_nuget_config() {
    final def nuget = [:]
    nuget.with(DefaultValues.nuget)
    assertThat(job.nugetConfig, allOf(
      hasProperty('configRef', equalTo(nuget.configRef)),
    ))
  }

  @Test void has_default_pip_config() {
    final pip = [:]
    pip.with(DefaultValues.pip)
    assertThat(job.pipConfig, allOf(
      hasProperty('configRef', equalTo(pip.configRef)),
    ))
  }

  @Test void is_configured_not_to_generate_job_parameters() {
    assertThat(job.generateJobParameters, is(false))
  }
}

class IntegrateServiceJobTest {
  final pipConfig = mock(PipConfig)
  final nugetConfig = mock(NugetConfig)
  final pipeline = mock(DummyPipeline)
  final job = new IntegrateServiceJob(pipeline)

  @Before void beforeEachTest() {
    job.pipConfig = pipConfig
    job.nugetConfig = nugetConfig
  }

  @Test void defines_env_vars_for_its_pip_config_with_its_pipeline() {
    when(pipConfig.defineEnvVars(pipeline)).thenReturn(['pip var 1', 'pip var 2'])
    assertThat(job.defineEnvVars(), is(equalTo(['pip var 1', 'pip var 2'])))
  }

  @Test void defines_volume_for_its_pip_config_with_its_pipeline() {
    when(pipConfig.defineVolumes(pipeline)).thenReturn(['pip volume 1', 'pip volume 2'])
    assertThat(job.defineVolumes(), is(equalTo(['pip volume 1', 'pip volume 2'])))
  }

  @Test void defines_env_vars_for_its_nuget_config_with_its_pipeline() {
    when(nugetConfig.defineEnvVars(pipeline)).thenReturn(['nuget var 1', 'nuget var 2'])
    assertThat(job.defineEnvVars(), is(equalTo(['nuget var 1', 'nuget var 2'])))
  }

  @Test void defines_volume_for_its_nuget_config_with_its_pipeline() {
    when(nugetConfig.defineVolumes(pipeline)).thenReturn(['nuget volume 1', 'nuget volume 2'])
    assertThat(job.defineVolumes(), is(equalTo(['nuget volume 1', 'nuget volume 2'])))
  }
}

class IntegrateServiceJobCheckoutTest {
  final pipeline = mock(DummyPipeline, CALLS_REAL_METHODS)
  final job = spy(new IntegrateServiceJob(pipeline))
  final source = [:]

  @Before void beforeEachTest() {
    doReturn(source).when(job).checkoutSource()
  }

  @Test void writes_file_contains_list_of_changes_files_under_source_directory() {
    source.with {
      dir = 'source directory'
      changedFiles = ['first', 'second']
    }
    job.checkout()
    verify(pipeline).writeFile(mapContains(
      file: 'source directory/.ci/changed-files',
      text: 'first\nsecond',
    ))
  }

  @Test void does_not_write_file_contains_list_of_changed_files_when_the_source_has_no_changed_files() {
    final tests = [
      null,
      [],
      [null],
      [''],
      [null, ''],
      ['', null],
    ]
    tests.each { files ->
      source.changedFiles = files
      job.checkout()
      verify(pipeline, never()).writeFile(any(Map))
    }
  }

  @Test void sets_job_source() {
    job.source = null
    job.checkout()
    assertThat(job.source,  allOf(
      is(not(null)),
      sameInstance(source),
    ))
  }
}

class IntegrateServiceJobPublishDockerImagesTest {
  final pipeline = mock(DummyPipeline, CALLS_REAL_METHODS)
  final job = spy(new IntegrateServiceJob(pipeline))
  final source = [:]
  final buildImage = [:]
  final serviceImage = [:]

  @Before void beforeEachTest() {
    doReturn(true).when(job).hasDeploymentJob()
  }

  @Test void tag_all_images_being_published_to_nexus() {
    job.publishDockerImages(source, buildImage, serviceImage)
    final image = ArgumentCaptor.forClass(Map)
    verify(job, times(2)).publishDockerImage(image.capture())
    assertThat(image.allValues, everyItem(
      hasEntry(equalTo('tag'), startsWith(job.nexusConfig.authority))
    ))
  }

  @Test void tag_all_images_with_the_service_name() {
    job.serviceConfig.service.name = 'service name'
    job.publishDockerImages(source, buildImage, serviceImage)
    final image = ArgumentCaptor.forClass(Map)
    verify(job, times(2)).publishDockerImage(image.capture())
    assertThat(image.allValues, everyItem(
      hasEntry(equalTo('tag'), containsString('service name'))
    ))
  }

  @Test void does_not_publish_docker_images_when_the_job_has_no_deployment_job() {
    doReturn(false).when(job).hasDeploymentJob()
    doNothing().when(job).markStageSkipped(any(String))
    job.publishDockerImages(source, buildImage, serviceImage)
    verify(job, never()).publishDockerImage(any(Map))
  }

  @Test void tag_all_images_with_artifact_version_tag() {
    final versionTag = 'version tag' 
    doReturn(versionTag).when(job).artifactVersionTag
    job.publishDockerImages(source, buildImage, serviceImage)
    final image = ArgumentCaptor.forClass(Map)
    verify(job, times(2)).publishDockerImage(image.capture())
    assertThat(image.allValues, everyItem(
      hasEntry(equalTo('tag'), containsString('version tag'))
    ))
  }
}

class IntegrateServiceJobSourceTest {
  final pipeline = mock(DummyPipeline, CALLS_REAL_METHODS)
  final job = spy(new IntegrateServiceJob(pipeline))

  @Test void source_version_gets_constructed_from_source_tag() {
    job.source = [tag: 'v1.2.3-4-gdeadbee']
    final version = new SemanticVersionBuilder().fromGitTag(job.source.tag).build()
    assertThat(job.sourceVersion, is(equalTo(version)))
  }

  @Test void source_version_is_zero_when_no_source() {
    job.source = null
    assertThat(job.sourceVersion, is(SemanticVersion.ZERO))
  }

  @Test void source_version_is_zero_when_source_has_no_tag() {
    job.source = [tag: null]
    assertThat(job.sourceVersion, is(SemanticVersion.ZERO))
  }

  @Test void artifact_version_is_zero_when_source_version_is_zero() {
    doReturn(SemanticVersion.ZERO).when(job).sourceVersion
    assertThat(job.artifactVersion, is(SemanticVersion.ZERO))
  }

  @Test void artifact_version_is_equal_to_source_version_when_source_version_has_no_builds() {
    final sourceVersion = new SemanticVersion(major: 1, minor: 2, patch: 3, prerelease: ['a', 1])
    doReturn(sourceVersion).when(job).sourceVersion
    assertThat(job.artifactVersion, is(equalTo(sourceVersion)))
  }

  @Test void artifact_version_contains_source_unix_timestamp_as_additional_prerelease_when_source_version_has_build() {
    job.source = [timestamp: 1234567]
    final sourceVersion = new SemanticVersion(major: 1, minor: 2, patch: 3,
      prerelease: ['a', 1], build: 'b')
    final expectedVersion = new SemanticVersion(major: 1, minor: 2, patch: 3,
      prerelease: ['a', 1, 'sut', 1234567], build: 'b')
    doReturn(sourceVersion).when(job).sourceVersion
    assertThat(job.artifactVersion, is(equalTo(expectedVersion)))
  }

  @Test void artifact_version_is_equal_to_source_version_when_source_version_has_build_and_source_has_no_timestamp() {
    job.source = [timestamp: null]
    final sourceVersion = new SemanticVersion(major: 1, minor: 2, patch: 3,
      prerelease: ['a', 1], build: 'b')
    doReturn(sourceVersion).when(job).sourceVersion
    assertThat(job.artifactVersion, is(equalTo(sourceVersion)))
  }

  @Test void packs_deployment_artifact_with_service_name_as_its_name_and_artifact_version_tag_as_its_tag() {
    // Arrange
    job.serviceConfig.service = 'service'
    doReturn('artifact version tag').when(job).artifactVersionTag
    job.tar = mock(TarTool)
    // Act
    final pack = job.packDeploymentArtifact([:])
    // Assert
    verify(job.tar).pack(any(Map), eq('service:artifact version tag.tgz'), any())
    assertThat(pack, allOf(
      hasEntry(equalTo('name'), equalTo('service')),
      hasEntry(equalTo('tag'), equalTo('artifact version tag')),
    ))
  }

  @Test void does_not_publish_deployment_artifact_when_it_has_no_depoyment_job() {
    doReturn(false).when(job).hasDeploymentJob()
    doNothing().when(job).markStageSkipped(any(String))
    job.curl = mock(CurlTool)
    job.publishDeploymentArtifact([:])
    verify(job.curl, never()).uploadFile(any(Map))
  }

  @Test void publishes_deployment_artifact_file_named_and_tagged_accordingly_to_the_artifact() {
    // Arrange
    doReturn(true).when(job).hasDeploymentJob()
    job.curl = mock(CurlTool)
    final artifact = [
      file: 'file',
      name: 'name',
      tag: 'tag',
    ]
    // Act
    job.publishDeploymentArtifact(artifact)
    // Assert
    final upload = ArgumentCaptor.forClass(Map)
    verify(job.curl).uploadFile(upload.capture())
    assertThat(upload.value, allOf(
      hasEntry(equalTo('file'), equalTo('file')),
      hasEntry(equalTo('url'), endsWith('/name/tag')),
    ))
  }

  @Test void builds_valid_artifact_version_tag_from_artifact_version() {
    final test = [
      [ SemanticVersion.ZERO, '0.0.0' ],
      [ new SemanticVersion(major: 1, minor: 2, patch: 3), '1.2.3' ],
      [ new SemanticVersion(major: 1, minor: 2, patch: 3, prerelease: ['a', 4]), '1.2.3-a.4' ],
      [ new SemanticVersion(major: 1, minor: 2, patch: 3, prerelease: ['a', 4], build: ['b', 5]), '1.2.3-a.4_b.5' ],
      [ new SemanticVersion(major: 1, minor: 2, patch: 3, build: ['b', 4]), '1.2.3_b.4' ],
    ]
    test.each { version, tag ->
      doReturn(version).when(job).artifactVersion
      assertThat("\n     For: ${ version }",
        job.artifactVersionTag, is(equalTo(tag)))
    }
  }

  @Test void checkout_source_returns_source_with_dir_refers_to_source_root_dir_under_checkout_dir() {
    job.sourceRootDir = 'source root dir'
    doReturn([dir: 'checkout dir']).when(job).runCheckoutStep()
    when(pipeline.fileExists('checkout dir/source root dir')).thenReturn(true)
    final source = job.checkoutSource()
    assertThat(source.dir, is(equalTo('checkout dir/source root dir')))
  }

  @Test void checkout_source_raises_an_error_when_source_root_dir_does_not_exists() {
    job.sourceRootDir = 'source root dir'
    doReturn([dir: 'checkout dir']).when(job).runCheckoutStep()
    when(pipeline.fileExists('checkout dir/source root dir')).thenReturn(false)
    job.checkoutSource()
    verify(pipeline).error(argThat(
      containsString('\'checkout dir/source root dir\' does not exist')))
  }
}

class IntegrateServiceJobInitializationTest {
  final job = new IntegrateServiceJob(mock(DummyPipeline))

  @Test void when_git_config_repository_is_not_defined_set_it_equal_to_service_source_name() {
    [
      null,
      '',
      ' ',
    ].each { repository ->
      job.gitConfig.repository = repository
      job.serviceConfig.service.sourceName = 'service source name'
      job.initializeJob()
      assertThat("\n     For: ${ repository }",
        job.gitConfig.repository, is(equalTo('service source name')))
    }
  }

  @Test void when_git_config_repository_is_defined_leave_it_intact() {
    job.serviceConfig.service.sourceName = 'service source name'
    job.gitConfig.repository = 'git repository'
    job.initializeJob()
    assertThat(job.gitConfig.repository, is(equalTo('git repository')))
  }

  @Test void when_service_name_is_not_defined_set_it_equal_to_git_config_repository() {
    [
      null,
      '',
      ' ',
    ].each { serviceName ->
      job.serviceConfig.service.name = serviceName
      job.gitConfig.repository = 'git config repository'
      job.initializeJob()
      assertThat(job.serviceConfig.service.name, is(equalTo('git config repository')))
    }
  }

  @Test void when_git_service_name_is_defined_leave_it_intact() {
    job.serviceConfig.service.name = 'service name'
    job.gitConfig.repository = 'git repository'
    job.initializeJob()
    assertThat(job.serviceConfig.service.name, is(equalTo('service name')))
  }
}
