package com.e4d.aws.sts

import com.e4d.shell.Shell

import org.junit.*
import org.mockito.ArgumentCaptor
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class AwsStsToolTest {
  final Shell shell = mock(Shell.class)
  final AwsStsTool tool = new AwsStsTool(shell)
  final def shellArgs = ArgumentCaptor.forClass(Map.class)
  final def shellEnvs = ArgumentCaptor.forClass(List.class)

  @Test void assume_role_script_starts_with_bash_shebang() {
    when(shell.execute(shellArgs.capture(), shellEnvs.capture())).thenReturn('{}')
    tool.assumeRole('', '')
    assertThat(shellArgs.value.script, startsWith('#!/usr/bin/env bash'))
  }
}
