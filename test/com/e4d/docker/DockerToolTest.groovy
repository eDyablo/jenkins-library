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

  @Test void push_with_only_image_specified_executes_correct_command() {
    [
      'image',
      'imge:tag',
    ].each { image ->
      // Arrange
      reset(shell)
      // Act
      docker.push(image)
      // Assert
      verify(shell).execute(
        argThat(
          hasEntry(
            equalTo('script'),
            allOf(
              stringContainsInOrder(
                'docker push ',
                image,
              ),
              not(containsString('docker rmi')),
            )
          )
        ),
        argThat(equalTo([]))
      )
    }
  }

  @Test void push_with_registry_and_image_specified_executes_correct_commands() {
    [
      'image',
      'imge:tag',
    ].each { image ->
      // Arrange
      reset(shell)
      // Act
      docker.push(image, registry: 'registry')
      // Assert
      verify(shell).execute(
        argThat(
          hasEntry(
            equalTo('script'),
            stringContainsInOrder(
              'docker login ', 'registry',
              'docker tag ', image, " registry/${ image }",
              'docker push ', " registry/${ image }",
              'docker rmi ', ' --no-prune=true ', " registry/${ image }",
            )
          )
        ),
        argThat(equalTo([]))
      )
    }
  }

  @Test void push_with_registry_correctly_uses_specified_username() {
    // Act
    docker.push('image', registry: 'registry', username: 'username')
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker login ', ' --username=${login_username} ' , ' registry',
            'docker tag ', ' image ', ' registry/image',
            'docker push ', ' registry/image',
          )
        )
      ),
      argThat(equalTo(['login_username=username']))
    )
  }

  @Test void push_with_registry_correctly_uses_specified_password() {
    // Act
    docker.push('image', registry: 'registry', password: 'password')
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker login ', ' --password=${login_password} ' , ' registry',
            'docker tag ', ' image ', ' registry/image',
            'docker push ', ' registry/image',
          )
        )
      ),
      argThat(equalTo(['login_password=password']))
    )
  }

  @Test void push_with_image_and_name_specified_executes_correct_commands() {
    // Act
    docker.push('image', name: 'name')
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker tag ', ' image ', ' name',
            'docker push ', ' name',
            'docker rmi ', ' --no-prune=true ', ' name',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void build_with_path_and_registry_executes_correct_commands() {
    // Act
    docker.build(path: 'path', registry: 'registry')
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker login ', 'registry',
            'docker build ', ' path',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void build_with_registry_correctly_uses_specified_username() {
    // Act
    docker.build(path: 'path', registry: 'registry', username: 'username')
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker login ', ' --username=${login_username} ' , ' registry',
            'docker build ', ' path',
          )
        )
      ),
      argThat(equalTo(['login_username=username']))
    )
  }

  @Test void build_with_registry_correctly_uses_specified_password() {
    // Act
    docker.build(path: 'path', registry: 'registry', password: 'password')
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker login ', ' --password=${login_password} ' , ' registry',
            'docker build ', ' path',
          )
        )
      ),
      argThat(equalTo(['login_password=password']))
    )
  }

  @Test void push_with_name_specified_does_not_execute_rmi_command_when_asked_to_keep_image() {
    // Act
    docker.push('image', name: 'name', keepImage: true)
    // Assert
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          not(containsString('docker rmi '))
        )
      ),
      argThat(equalTo([]))
    )
  }
}
