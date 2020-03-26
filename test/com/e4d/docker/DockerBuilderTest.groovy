package com.e4d.docker

import com.e4d.pipeline.DummyPipeline
import com.e4d.pipeline.PipelineShell
import com.e4d.shell.Shell
import org.junit.*
import org.mockito.ArgumentCaptor
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class DockerBuilderTest {
  DockerBuilder builder
  final def pipeline = mock(DummyPipeline.class)
  final def shell = mock(Shell.class)
  final def shellArgs = ArgumentCaptor.forClass(Map.class)
  final def shellEnvs = ArgumentCaptor.forClass(List.class)

  DockerBuilderTest() {
    builder = new DockerBuilder(pipeline, shell)
  }

  def getShellScript() {
    shellArgs.value.script
  }

  def getShellCommands() {
    shellScript.split('\n') as List<String>
  }

  @Test void single_phase_build_does_not_login_when_no_registry_used() {
    // Given
    when(pipeline.fileExists(any())).thenReturn(false)
    when(shell.execute(any(Map.class), any(List.class))).thenReturn('')
    // When
    builder.build([:], '')
    // Then
    verify(shell).execute(shellArgs.capture(), shellEnvs.capture())
    assertThat(shellCommands, not(hasItem(startsWith('docker login'))))
  }

  @Test void multi_phase_build_does_not_login_when_no_registry_used() {
    // Given
    when(pipeline.fileExists(any())).thenReturn(true)
    when(shell.execute(any(Map.class), any(List.class))).thenReturn('')
    // When
    builder.build([:], '')
    // Then
    verify(shell).execute(shellArgs.capture(), shellEnvs.capture())
    assertThat(shellCommands, not(hasItem(startsWith('docker login'))))
  }

  @Test void single_phase_build_does_login_when_registry_used() {
    // Given
    when(pipeline.fileExists(any())).thenReturn(false)
    when(shell.execute(any(Map.class), any(List.class))).thenReturn('')
    // When
    builder.useRegistry('registry', 'user', 'password')
    builder.build([:], '')
    // Then
    verify(shell).execute(shellArgs.capture(), shellEnvs.capture())
    assertThat(shellCommands, hasItem(allOf(
      startsWith('docker login'),
      containsString('registry'),
      containsString('--username=user'),
      containsString('--password=password')
    )))
  }

  @Test void multi_phase_build_does_login_when_registry_used() {
    // Given
    when(pipeline.fileExists(any())).thenReturn(true)
    when(shell.execute(any(Map.class), any(List.class))).thenReturn('')
    // When
    builder.useRegistry('registry', 'user', 'password')
    builder.build([:], '')
    // Then
    verify(shell).execute(shellArgs.capture(), shellEnvs.capture())
    assertThat(shellCommands, hasItem(allOf(
      startsWith('docker login'),
      containsString('registry'),
      containsString('--username=user'),
      containsString('--password=password')
    )))
  }
}
