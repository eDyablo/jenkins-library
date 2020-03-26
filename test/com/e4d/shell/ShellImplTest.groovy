package com.e4d.shell

import org.junit.*
import org.junit.rules.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class ShellImplTest extends ShellImpl {
  int exitCalls = 0
  int executeCalls = 0

  @Rule public final ExpectedException thrown = ExpectedException.none();

  def execute(Map args, List env) {
    executeCalls++
  }

  def readFile(Map args) {
  }

  def writeFile(Map args) {
  }

  void exit() {
    exitCalls++
  }

  @Test void with_shell_calls_exit_when_code_block_is_empty() {
    withShell {}
    assertThat(exitCalls, is(equalTo(1)))
  }

  @Test void with_shell_gives_access_to_the_shell() {
    withShell {
      it.execute('script')
    }
    assertThat(executeCalls, is(equalTo(1)))
  }

  @Test void with_shell_calls_exit_when_code_block_throws() {
    thrown.expect(Exception)
    withShell {
      throw new Exception()
    }
    assertThat(exitCalls, is(equalTo(1)))
  }
}
