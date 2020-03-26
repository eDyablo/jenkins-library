package com.e4d.docker

import com.e4d.shell.Shell
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class DockerToolTest {
  final shell = mock(Shell)
  final docker = spy(new DockerTool(shell))

  @Test void builds_dockerfile_from_current_directory_when_no_options_defined() {
    docker.build()
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder('docker build', ' .')
        )
      ),
      argThat(instanceOf(List))
    )
  }

  @Test void builds_dockerfile_from_specified_path() {
    docker.build(path: 'path')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder('docker build',  ' path')
        )
      ),
      argThat(instanceOf(List))
    )
  }

  @Test void builds_with_specified_build_args() {
    docker.build(
      path: 'path',
      buildArgs: [
        arg1: 1,
        arg2: 2,
      ],
    )
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker build',
            ' --build-arg=\'arg1=1\' ',
            ' --build-arg=\'arg2=2\' ',
            'path',
          )
        )
      ),
      argThat(instanceOf(List))
    )
  }

  @Test void build_uses_network_when_it_is_specified_in_options() {
    docker.build(
      path: 'path',
      network: 'network',
    )
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker build',
            ' --network=network ',
            'path',
          )
        )
      ),
      argThat(instanceOf(List))
    )
  }

  @Test void build_set_no_network_when_it_is_not_specified_in_options() {
    // Act
    docker.build(path: 'path')
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          not(containsString('--network'))
        )
      ),
      argThat(instanceOf(List))
    )
  }

  @Test void build_returns_image_id_that_it_reads_from_image_id_file() {
    // Arrange
    doReturn('image id file').when(docker).getImageIdFile('path')
    doReturn('image id').when(shell).readFile(argThat(hasEntry('file', 'image id file')))
    // Act
    final id = docker.build(path: 'path')
    // Assert
    assertThat(id, is(equalTo('image id')))
  }

  @Test void login_uses_specified_server() {
    docker.login(server: 'server')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker login',
            'server',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void login_executes_default_login_when_no_option_set() {
    docker.login()
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          allOf(
            containsString('docker login'),
            not(containsString('null')),
            not(containsString('  ')),
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void login_passes_specified_username_via_environment_variables() {
    docker.login(username: 'user')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker login',
            '--username=${login_username}',
          )
        )
      ),
      argThat(equalTo(['login_username=user']))
    )
  }

  @Test void login_passes_specified_password_via_environment_variables() {
    docker.login(password: 'password')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker login',
            '--password=${login_password}',
          )
        )
      ),
      argThat(equalTo(['login_password=password']))
    )
  }
}
