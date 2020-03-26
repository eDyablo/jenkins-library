package com.e4d.python

import com.e4d.python.PytestToolTest
import com.e4d.shell.Shell

import org.junit.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any
import org.mockito.ArgumentCaptor

class PytestToolTest extends PytestTool {
  final def shellArgs = ArgumentCaptor.forClass(Map.class)
  final def shellEnvs = ArgumentCaptor.forClass(List.class)

  PytestToolTest() {
    super(mock(Object.class), mock(Shell.class))
  }

  def getShellScript() {
    shellArgs.value.script
  }

  def getShellCommands() {
    shellScript.split('\n') as List<String>
  }

  def test(Map options=[:]) {
    super.test(options)
    verify(shell).execute(shellArgs.capture(), shellEnvs.capture())
  }

  def test(Map options=[:], String baseDir) {
    super.test(options, baseDir)
    verify(shell).execute(shellArgs.capture(), shellEnvs.capture())
  }

  @Test void test_uses_current_dir_when_no_base_dir_specified() {
    test()
    assertThat(shellCommands, hasItem('base_dir="."'))
  }

  @Test void test_uses_specified_base_dir() {
    test('base-dir')
    assertThat(shellCommands, hasItem('base_dir="base-dir"'))
  }

  @Test void test_does_not_specify_find_expression_by_default() {
    test()
    assertThat(shellCommands, hasItem('find_expression=""'))
  }

  @Test void test_includes_specified_file_name_pattern() {
    test(includeFileNames: 'name')
    assertThat(shellCommands, hasItem(
      'find_expression="\\( -name \'name\' \\)"'))
  }

  @Test void test_includes_specified_multiple_file_name_patterns() {
    test(includeFileNames: ['first', 'second'])
    assertThat(shellCommands, hasItem(
      'find_expression="\\( -name \'first\' -or -name \'second\' \\)"'))
  }

  @Test void test_excludes_specified_file_name_pattern() {
    test(excludeFileNames: 'name')
    assertThat(shellCommands, hasItem(
      'find_expression="\\( -not -name \'name\' \\)"'))
  }

  @Test void test_excludes_specified_multiple_file_name_patterns() {
    test(excludeFileNames: ['first', 'second'])
    assertThat(shellCommands, hasItem(
      'find_expression="\\( -not -name \'first\' -and -not -name \'second\' \\)"'))
  }

  @Test void test_includes_and_excludes_file_name_patterns_in_one_expression() {
    test(
      includeFileNames: ['inc-a', 'inc-b'],
      excludeFileNames: ['exc-a', 'exc-b'],
    )
    assertThat(shellCommands, hasItem(
      'find_expression="\\( -name \'inc-a\' -or -name \'inc-b\' \\) -and \\( -not -name \'exc-a\' -and -not -name \'exc-b\' \\)"'))
  }

  @Test void test_searches_files_in_the_base_according_to_the_find_expressions() {
    test()
    assertThat(shellCommands, hasItem(
      'test_files="$(eval "find "${base_dir}" "${find_expression}"")"'
    ))
  }

  @Test void test_uses_only_test_files() {
    test()
    assertThat(shellScript, containsString('pytest ${test_files}'))
  }

  @Test void test_excludes_specified_path_pattern() {
    test(excludePaths: 'path')
    assertThat(shellCommands, hasItem('find_expression="\\( -not -path \'path\' \\)"'))
  }

  @Test void test_excludes_specified_multiple_path_patterns() {
    test(excludePaths: ['first', 'second'])
    assertThat(shellCommands, hasItem('find_expression="\\( -not -path \'first\' -and -not -path \'second\' \\)"'))
  }

  @Test void test_excludes_file_name_and_path_when_both_are_specified() {
    test(excludeFileNames: 'name', excludePaths: 'path')
    assertThat(shellCommands, hasItem('find_expression="\\( -not -name \'name\' \\) -and \\( -not -path \'path\' \\)"'))
  }

  @Test void test_includes_file_name_and_excludes_file_name_and_path_when_all_three_are_specified() {
    test(includeFileNames: 'name', excludeFileNames: 'name', excludePaths: 'path')
    assertThat(shellCommands, hasItem('find_expression="\\( -name \'name\' \\) -and \\( -not -name \'name\' \\) -and \\( -not -path \'path\' \\)"'))
  }
}
