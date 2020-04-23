package com.e4d.job

import com.e4d.build.SemanticVersion
import com.e4d.docker.DockerTool
import com.e4d.git.GitSourceReference
import com.e4d.pipeline.DummyPipeline
import java.net.URI
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class IntegrateDockerImageJobTest {
  final pipeline = spy(DummyPipeline)
  final job = spy(new IntegrateDockerImageJob(pipeline))

  @Before void beforeEachTest() {
    reset(job)
    doReturn([:]).when(job).checkoutSource(argThat(any(Map)))
    doNothing().when(job).markStageSkipped(argThat(instanceOf(String)))
  }

  @Test void run_does_checkout_from_git_source_in_checkout_stage() {
    // Arrange
    job.gitSourceRef = new GitSourceReference('source')
    // Act
    job.run()
    // Assert
    final order = inOrder(pipeline, job)
    order.verify(pipeline).stage(argThat(equalTo('checkout')), argThat(any(Closure)))
    order.verify(job).checkout()
  }

  @Test void run_skips_checkout_when_no_repository_defined() {
    // Assert
    job.gitSourceRef = new GitSourceReference()
    // Act
    job.run()
    // Assert
    verify(job, never()).checkout()
    verify(job).markStageSkipped('checkout')
  }

  @Test void run_studies_source_in_study_stage() {
    // Assert
    job.source = [dir: 'source']
    // Act
    job.run()
    // Assert
    final order = inOrder(pipeline, job)
    order.verify(pipeline).stage(argThat(equalTo('study')), argThat(any(Closure)))
    order.verify(job).study()
  }

  @Test void run_skips_study_when_no_source() {
    // Assert
    job.source = null
    // Act
    job.run()
    // Assert
    verify(job, never()).study()
    verify(job).markStageSkipped('study')
  }

  @Test void study_returns_source_dockerfile_if_it_exists_in_source_dir() {
    // Arrange
    job.source = [dir: 'source dir']
    doReturn(true).when(pipeline).fileExists('source dir/Dockerfile')
    // Act & Assert
    assertThat(job.study(), hasEntry('dockerfile', 'source dir/Dockerfile'))
  }

  @Test void study_returns_no_dockerfile_if_it_does_not_exist_in_source_dir() {
    // Arrange
    job.source = [dir: 'source dir']
    doReturn(false).when(pipeline).fileExists('source dir/Dockerfile')
    // Act & Assert
    assertThat(job.study(), not(hasEntry('dockerfile', null)))
  }

  @Test void has_default_nexus_config_when_created() {
    // Arrange
    final nexus = [:]
    nexus.with(DefaultValues.nexus)
    // Act
    final job = new IntegrateNugetPackageJob(pipeline)
    // Assert
    assertThat(job.nexusConfig, allOf(
      hasProperty('baseUrl', equalTo(nexus.baseUrl)),
      hasProperty('credsId', equalTo(nexus.credsId)),
    ))
  }

  @Test void run_does_build_in_build_stage() {
    // Arrange
    job.source = [dir: 'source']
    doReturn(dockerfile: 'dockerfile').when(job).study()
    // Act
    job.run()
    // Assert
    verify(pipeline).stage(argThat(equalTo('build')), argThat(any(Closure)))
    verify(job).build()
  }

  @Test void run_does_build_after_study() {
    // Arrange
    job.source = [dir: 'source']
    doReturn(dockerfile: 'dockerfile').when(job).study()
    // Act
    job.run()
    // Assert
    final order = inOrder(job)
    order.verify(job).study()
    order.verify(job).build()
  }

  @Test void run_does_not_build_and_marks_build_stage_skipped_when_no_source() {
    [
      null, [:],
    ].each { source ->
      beforeEachTest()
      // Arrange
      job.source = source
      // Act
      job.run()
      // Assert
      verify(job, never()).build()
      verify(job).markStageSkipped('build')
    }
  }

  @Test void run_does_not_build_and_marks_build_stage_skipped_when_no_dockerfile() {
    [
      null, '',
    ].each { dockerfile ->
      beforeEachTest()
      // Arrange
      job.source = [dockerfile: dockerfile]
      // Act
      job.run()
      // Assert
      verify(job, never()).build()
      verify(job).markStageSkipped('build')
    }
  }

  @Test void builds_from_source_directory() {
    // Arrange
    job.source = [dir: 'source directory']
    job.docker = mock(DockerTool)
    // Act
    job.build()
    // Assert
    verify(job.docker).build(argThat(
      hasEntry('path', 'source directory')
    ))
  }

  @Test void build_uses_host_network() {
    // Arrange
    job.source = [dir: 'source']
    job.docker = mock(DockerTool)
    // Act
    job.build()
    // Assert
    verify(job.docker).build(
      argThat(
        hasEntry('network', 'host')
      )
    )
  }

  @Test void build_uses_specified_docker_registry_and_creds() {
    // Arrange
    job.docker = mock(DockerTool)
    job.source = [:]
    job.dockerRegistry = new URI('registry')
    job.dockerRegistry.userInfo = 'user:password'
    // Act
    job.build()
    // Assert
    verify(job.docker).build(
      argThat(allOf(
        hasEntry('registry', job.dockerRegistry),
        hasEntry('username', 'user'),
        hasEntry('password', 'password'),
      ))
    )
  }

  @Test void initialize_job_set_docker_registry_from_nexus_config_when_the_registry_is_not_defined() {
    // Arrange
    job.dockerRegistry = null
    job.nexusConfig.baseUrl = 'nexus-url'
    job.nexusConfig.port = 10
    job.nexusConfig.credsId = 'nexus-creds'
    doReturn(['nexus-user', 'nexus-password']).when(job).getUsernamePassword('nexus-creds')
    // Act
    job.initializeJob()
    // Assert
    assertThat(job.dockerRegistry,
      is(allOf(
        equalTo(new URI('nexus-url:10')),
        hasProperty('userInfo', equalTo('nexus-user:nexus-password')),
      ))
    )
  }

  @Test void initialize_job_leaves_docker_registry_intact_if_is_defined() {
    // Arrange
    job.dockerRegistry = new URI('intact')
    // Act
    job.initializeJob()
    // Assert
    assertThat(job.dockerRegistry, hasToString('intact'))
  }

  @Test void run_does_delivery_in_deliver_stage_when_image_id_is_defined() {
    // Arrange
    job.source = [dockerfile: 'dockerfile']
    doReturn('image id').when(job).build()
    // Act
    job.run()
    // Assert
    verify(pipeline).stage(argThat(equalTo('deliver')), argThat(any(Closure)))
    verify(job).deliver()
  }

  @Test void run_does_not_deliver_and_marks_deliver_stage_skipped_when_no_image_id() {
    [
      null, '',
    ].each { imageId ->
      beforeEachTest()
      // Arrange
      job.source = [dockerfile: 'dockerfile']
      doReturn(imageId).when(job).build()
      // Act
      job.run()
      // Assert
      verify(job, never()).deliver()
      verify(job).markStageSkipped('deliver')
    }
  }

  @Test void can_deliver_when_image_id_is_defined() {
    job.imageId = 'image id'
    assertThat(job.canDeliver, is(true))
  }

  @Test void can_not_deliver_when_image_id_is_not_defined() {
    [
      null,
      '',
    ].each { imageId ->
      job.imageId = imageId
      assertThat(job.canDeliver, is(false))
    }
  }

  @Test void can_not_deliver_when_dissalow_to_publish_prerelease_version_and_artifact_version_is_prerelease() {
    [
      new SemanticVersion(prerelease: 'a'),
      new SemanticVersion(major: 1, prerelease: 'a'),
      new SemanticVersion(major: 1, minor: 2, prerelease: 'a'),
      new SemanticVersion(major: 1, patch: 3, prerelease: 'a'),
    ].each { version ->
      job.imageId = 'image id'
      job.publishPrereleaseVersion = false
      doReturn(version).when(job).artifactVersion
      assertThat(job.canDeliver, is(false))
    }
  }

  @Test void can_deliver_when_dissalow_to_publish_prerelease_version_and_artifact_version_is_not_prerelease() {
    [
      new SemanticVersion(),
      new SemanticVersion(major: 1),
      new SemanticVersion(major: 1, minor: 2),
      new SemanticVersion(major: 1, patch: 2),
      new SemanticVersion(major: 1, minor: 2, patch: 3, build: 'b'),
    ].each { version ->
      job.imageId = 'image id'
      job.publishPrereleaseVersion = false
      doReturn(version).when(job).artifactVersion
      assertThat(job.canDeliver, is(true))
    }
  }

  @Test void delivers_image_with_name_started_with_artifact_base_name_when_it_is_defined() {
    // Arrange
    job.artifactBaseName = 'artifact base name'
    job.imageId = 'image id'
    job.docker = mock(DockerTool)
    // Act
    job.deliver()
    // Assert
    verify(job.docker).push(
      argThat(
        hasEntry(
          equalTo('name'),
          startsWith('artifact base name:'),
        )
      ),
      argThat(equalTo('image id'))
    )
  }

  @Test void delivers_image_with_name_started_with_repository_when_artifact_base_name_when_is_not_defined() {
    [
      null, ''
    ].each { artifactBaseName ->
      // Arrange
      job.artifactBaseName = artifactBaseName
      job.gitSourceRef = new GitSourceReference('repository')
      job.imageId = 'image id'
      job.docker = mock(DockerTool)
      // Act
      job.deliver()
      // Assert
      verify(job.docker).push(
        argThat(
          hasEntry(
            equalTo('name'),
            startsWith('repository:'),
          )
        ),
        argThat(equalTo('image id'))
      )
    }
  }
}
