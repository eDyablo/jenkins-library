package com.e4d.docker

import com.e4d.shell.Shell
import java.time.Duration
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class DockerContainerShellTest {
  final host = mock(Shell)

  @Test void execute_starts_new_detached_auto_removable_container_for_specified_image_when_container_id_is_not_defined() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      image: 'image',
      lifetime: Duration.ofSeconds(1),
    ))
    when(shell.getContainerName('image')).thenReturn('container')
    // Act
    shell.execute('command')
    // Assert
    verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker create --rm --name=container',
            '--entrypoint=sh', 
            'image',
            '-c \'sleep 1\'',
            'docker start container',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void executes_script_by_copying_it_into_container_and_running_it_via_shell() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      containerId: 'container',
      hostScriptDir: 'host dir',
      guestScriptDir: 'guest dir',
      shellProgram: 'shell',
    ))
    when(shell.getScriptName('command')).thenReturn('command script')
    // Act
    shell.execute('command')
    // Assert
    verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker cp \'host dir/command script\' container:\'guest dir/command script\'',
            'docker exec', 'container shell \'guest dir/command script\'',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void exit_after_exexute_forcibly_remove_shells_container_when_the_shell_owns_it() {
    // Arrange
    final shell = spy(new DockerContainerShell(hostShell: host, image: 'image'))
    when(shell.getContainerName('image')).thenReturn('container')
    // Act
    shell.execute('command')
    shell.exit()
    // Assert
    verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          equalTo('docker rm --force container')
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void exit_does_nothing_when_shell_does_not_own_container() {
    // Arrange
    final shell = spy(new DockerContainerShell(hostShell: host, containerId: 'container'))
    // Act
    shell.execute('command')
    shell.exit()
    // Assert
    verify(host, never()).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString('docker rm --force')
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void exit_does_nothing_when_shell_owns_container_but_no_execution_has_happened_before_exit() {
    // Arrange
    final shell = spy(new DockerContainerShell(hostShell: host, containerId: 'container'))
    // Act
    shell.exit()
    // Assert
    verify(host, never()).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString('docker rm --force')
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void exit_does_nothing_when_gets_call_second_time() {
    // Arrange
    final shell = spy(new DockerContainerShell(hostShell: host, image: 'image'))
    when(shell.getContainerName('image')).thenReturn('container')
    // Act
    shell.execute('command')
    shell.exit()
    shell.exit()
    // Assert
    verify(host, times(1)).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString('docker rm --force')
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void execute_writes_script_into_file_on_host_before_execute_in_in_container() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      hostScriptDir: 'host dir',
    ))
    when(shell.getScriptName('command')).thenReturn('command script')
    // Act
    shell.execute('command')
    // Assert
    final order = inOrder(host)
    order.verify(host).writeFile(
      argThat(allOf(
        hasEntry(
          equalTo('file'),
          equalTo('host dir/command script')
        ),
        hasEntry(
          equalTo('text'),
          equalTo('command')
        )
      ))
    )
    order.verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString('docker exec')
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void execute_passes_specified_env_variables_as_options_for_exec_command() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      containerId: 'container',
    ))
    // Act
    shell.execute('command', ['first=1', 'second=2'])
    // Assert
    verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker exec ',
            '--env=\'first=1\' --env=\'second=2\'',
            ' container'
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void execute_creates_container_from_image_with_specified_network() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      image: 'image',
      network: 'network',
    ))
    // Act
    shell.execute('command')
    // Assert
    verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker create', '--network=network', 'image',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void execute_logs_in_to_specified_registry_when_creates_container_image() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      image: 'image',
      registry: 'registry',
    ))
    // Act
    shell.execute('command')
    // Assert
    verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker login registry',
            'docker create',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void execute_uses_specified_registry_username_and_password_when_login_to_the_registry() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      image: 'image',
      registry: 'registry',
      registryCreds: [
        username: 'username',
        password: 'password'
      ]
    ))
    // Act
    shell.execute('command')
    // Assert
    verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker login --username="${registry_username}" --password="${registry_password}" registry',
          )
        )
      ),
      argThat(contains('registry_username=username', 'registry_password=password'))
    )
  }

  @Test void execute_set_specified_labels_when_creates_container_for_the_image() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      image: 'image',
      labels: ['l1': 'one', 'l2': 'two'],
    ))
    // Act
    shell.execute('command')
    // Assert
    verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker create',
            '--label=\'l1=one\'',
            '--label=\'l2=two\'',
            'image',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void execute_gets_std_output_from_host_shell_when_requested() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      image: 'image',
    ))
    // Act
    shell.execute(script: 'command', returnStdout: true)
    // Assert
    verify(host).execute(
      argThat(hasEntry('returnStdout', true)),
      argThat(equalTo([]))
    )
  }

  @Test void execute_does_not_create_container_on_second_call() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      image: 'image',
    ))
    // Act
    shell.execute(script: 'first')
    shell.execute(script: 'second')
    // Assert
    verify(host, times(1)).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString('docker create')
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void execute_creates_container_when_gets_called_after_exit() {
    // Arrange
    final shell = spy(new DockerContainerShell(
      hostShell: host,
      image: 'image',
    ))
    // Act
    shell.execute(script: 'first')
    shell.exit()
    shell.execute(script: 'second')
    // Assert
    verify(host, times(2)).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString('docker create')
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void execute_does_not_do_login_when_creates_container_image_when_no_registry_specified() {
    [
      null, '',
    ].each { registry ->
      reset(host)
      // Arrange
      final shell = spy(new DockerContainerShell(
        hostShell: host,
        image: 'image',
        registry: registry,
      ))
      // Act
      shell.execute('command')
      // Assert
      verify(host).execute(
        argThat(
          hasEntry(
            equalTo('script'),
            allOf(
              not(containsString('docker login')),
              not(containsString('null')),
            )
          )
        ),
        argThat(equalTo([]))
      )
    }
  }

  @Test void download_file() {
    // Arrange
    final shell = new DockerContainerShell(
      hostShell: host,
      containerId: 'container',
    )
    // Act
    shell.downloadFile('file', 'destination')
    // Assert
    verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker cp ',
            ' container:\'file\' ',
            '\'destination\'',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void upload_file() {
    // Arrange
    final shell = new DockerContainerShell(
      hostShell: host,
      containerId: 'container',
    )
    // Act
    shell.uploadFile('file', 'destination')
    // Assert
    verify(host).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'docker cp ',
            ' \'file\' ',
            ' container:\'destination\'',
          )
        )
      ),
      argThat(equalTo([]))
    )
  }
}
