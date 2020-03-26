package com.e4d.docker

import com.e4d.shell.Shell

import org.junit.*
import org.mockito.ArgumentCaptor
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class DockerImageTest {
  final Shell shell = mock(Shell.class)
  final DockerImageShell imageShell = new DockerImageShell(shell, '')
  final def shellArgs = ArgumentCaptor.forClass(Map.class)
  final def shellEnvs = ArgumentCaptor.forClass(List.class)

  @Test void execute_starts_with_bash_shebang() {
    when(shell.execute(shellArgs.capture(), shellEnvs.capture())).thenReturn('{}')
    when(shell.writeFile(any(Map.class))).thenReturn('')
    imageShell.execute('')
    assertThat(shellArgs.value.script, startsWith('#!/usr/bin/env bash'))
  }
}
