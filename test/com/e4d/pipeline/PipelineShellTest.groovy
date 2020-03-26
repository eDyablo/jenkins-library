package com.e4d.pipeline

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class PipelineShellTest {
  final pipeline = spy(DummyPipeline)
  final shell = new PipelineShell(pipeline)

  @Test void execute_runs_script_using_pipeline_sh() {
    shell.execute([script: 'script'])
    verify(pipeline).sh(argThat(hasEntry(
      equalTo('script'), containsString('script'))))
  }

  @Test void execute_adds_bash_shebang_to_the_script_when_there_is_no_shebang() {
    shell.execute([script: 'script'])
    verify(pipeline).sh(argThat(hasEntry(
      equalTo('script'), startsWith('#!/usr/bin/env bash'))))
  }

  @Test void execute_does_not_add_bash_shebang_when_there_is_shebang_already() {
    shell.execute([script: '#!shebang'])
    verify(pipeline).sh(argThat(hasEntry(
      equalTo('script'), not(containsString('#!/usr/bin/env bash')))))
  }
}
