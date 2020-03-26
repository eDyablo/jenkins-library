package com.e4d.file

import com.e4d.shell.Shell
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class FindToolTest {
  final shell = mock(Shell)
  final finder = spy(new FindTool(shell))

  @Test void searches_under_specified_directory() {
    // Act
    finder.find(under: 'directory')
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          equalTo('find directory')
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void searches_under_current_directory_by_default() {
    // Act
    finder.find()
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          equalTo('find')
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void returns_list_of_found_files_extracted_from_shell_output() {
    // Arrange
    doReturn('''
      first
      second
    '''.stripIndent().trim()).when(shell).execute(
      argThat(allOf(
        hasEntry('script', 'find'),
        hasEntry('returnStdout', true),
      )),
      argThat(equalTo([]))
    )
    // Act & Assert
    assertThat(finder.find(), is(equalTo(['first', 'second'])))
  }

  @Test void returns_empty_list_when_no_output_from_shell() {
    [
      null, '',
    ].each { output ->
      // Arrange
      doReturn(output).when(shell).execute(
        argThat(allOf(
          hasEntry('script', 'find'),
          hasEntry('returnStdout', true),
        )),
        argThat(equalTo([]))
      )
      // Act & Assert
      assertThat("\n     For: '${ output }'",
        finder.find(), is(equalTo([])))
    }
  }

  @Test void can_search_by_regular_expression() {
    // Act
    finder.find(regex: /^.+$/, under: 'directory')
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'find ',
            ' directory',
            ' -regex \'^.+$\'',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void can_search_by_name_pattern() {
    // Act
    finder.find(name: 'name pattern', under: 'directory')
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'find ',
            ' directory ',
            ' -name \'name pattern\'',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }
}
