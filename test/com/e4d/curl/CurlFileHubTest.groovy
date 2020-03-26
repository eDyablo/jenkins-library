package com.e4d.curl

import com.e4d.shell.Shell
import org.junit.*
import org.junit.rules.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class CurlFileHubTest {
  final shell = mock(Shell)
  final hub = new CurlFileHub(shell)

  @Rule public final ExpectedException thrown = ExpectedException.none()

  @Test void uploads_specified_file_to_specified_destination() {
    hub.uploadFile(file: 'file', destination: 'destination')
    final scriptMatch = stringContainsInOrder(
      'curl', '--upload-file', 'file', 'destination')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), scriptMatch)), any(List))
  }

  @Test void uploads_file_using_user_and_password() {
    hub.uploadFile(file: 'file', user: 'user', password: 'password')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), containsString('--user "${user}"'))),
      argThat(hasItem(equalTo('user=user:password'))))
  }

  @Test void upload_file_does_not_use_user_and_password_when_they_are_not_specified() {
    hub.uploadFile(file: 'file')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), not(containsString('--user "${curl.user}"')))),
      argThat(equalTo([])))
  }

  @Test void upload_file_fails_on_http_error() {
    hub.uploadFile(file: 'file')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), containsString('--fail'))),
      any(List))
  }

  @Test void upload_file_throws_when_options_are_not_specified() {
    thrown.expect(Exception)
    thrown.expectMessage('File is not specified')
    hub.uploadFile()
  }

  @Test void upload_file_throws_when_file_is_not_specified() {
    thrown.expect(Exception)
    thrown.expectMessage('File is not specified')
    hub.uploadFile([file: null])
  }

  @Test void upload_file_throws_when_file_is_empty() {
    thrown.expect(Exception)
    thrown.expectMessage('File is not specified')
    hub.uploadFile([file: ''])
  }

  @Test void upload_file_throws_when_file_is_whitespace() {
    thrown.expect(Exception)
    thrown.expectMessage('File is not specified')
    hub.uploadFile([file: ' '])
  }

  @Test void downloads_specified_file() {
    hub.downloadFile(file: 'file')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'),
        stringContainsInOrder('curl', 'file'))), any(List))
  }

  @Test void download_file_fails_on_http_error() {
    hub.downloadFile(file: 'file')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), containsString('--fail'))),
      any(List))
  }

  @Test void downloads_file_using_user_and_password() {
    hub.downloadFile(file: 'file', user: 'user', password: 'password')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), containsString('--user "${user}"'))),
      argThat(hasItem(equalTo('user=user:password'))))
  }

  @Test void download_file_does_not_use_user_and_password_when_they_are_not_specified() {
    hub.downloadFile(file: 'file')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), not(containsString('--user "${curl.user}"')))),
      argThat(equalTo([])))
  }

  @Test void download_file_throws_when_options_are_not_specified() {
    thrown.expect(Exception)
    thrown.expectMessage('File is not specified')
    hub.downloadFile()
  }

  @Test void download_file_uses_specified_destination_as_output() {
    hub.downloadFile(file: 'file', destination: 'destination')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), stringContainsInOrder('--output', 'destination'))),
      any(List)
    )
  }

  @Test void download_file_has_no_output_when_destination_is_not_set() {
    [
      null, '', ' '
    ].each { destination ->
      reset(shell)
      hub.downloadFile(file: 'file', destination: destination)
      verify(shell).execute(
        argThat(hasEntry(equalTo('script'), not(containsString('--output')))),
        any(List)
      )
    }
  }
}
