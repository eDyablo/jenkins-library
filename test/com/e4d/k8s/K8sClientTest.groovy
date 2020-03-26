package com.e4d.k8s

import com.e4d.pipeline.DummyPipeline

import org.junit.*
import org.junit.rules.*
import org.mockito.ArgumentCaptor
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class K8sClientTest {
  final def shArgs = ArgumentCaptor.forClass(Map.class)
  final def shScript = ArgumentCaptor.forClass(String.class)
  final def pipeline = mock(DummyPipeline.class)
  final def client = new K8sClient(pipeline)

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test void newly_created_hase_default_context() {
    assertThat(client.context, is('default'))
  }

  @Test void register_docker_registry_creates_docker_registry_secret_with_specified_values() {
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: 'password')
    verify(pipeline).sh(shArgs.capture())
    assertThat(shArgs.value.script, allOf(
      containsString('create secret'),
      containsString('docker-registry secret'),
      containsString('--docker-server=server'),
      containsString('--docker-username=user'),
      containsString('--docker-password=password'),
    ))
  }

  @Test void register_docker_registry_uses_script_that_srarts_with_bash_shebang() {
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: 'password')
    verify(pipeline).sh(shArgs.capture())
    assertThat(shArgs.value.script, startsWith('#!/usr/bin/env bash'))
  }

  @Test void register_docker_registry_does_not_use_namespace_if_it_is_not_specified() {
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: 'password')
    verify(pipeline).sh(shArgs.capture())
    assertThat(shArgs.value.script, not(containsString('--namespace=')))
  }

  @Test void register_docker_registry_uses_specified_namespace_when_it_is_not_empty() {
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: 'password',
      namespace: 'namespace')
    verify(pipeline).sh(shArgs.capture())
    assertThat(shArgs.value.script, containsString('--namespace=namespace'))
  }

  @Test void register_docker_registry_does_not_use_namespace_when_specified_namespace_is_empty() {
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: 'password',
      namespace: '')
    verify(pipeline).sh(shArgs.capture())
    assertThat(shArgs.value.script, not(containsString('--namespace=')))
  }

  @Test void register_docker_registry_does_not_use_namespace_when_specified_namespace_is_whitespace() {
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: 'password',
      namespace: ' \t  ')
    verify(pipeline).sh(shArgs.capture())
    assertThat(shArgs.value.script, not(containsString('--namespace=')))
  }

  @Test void register_docker_registry_uses_specified_context_if_it_is_not_empty() {
    client.useContext('context')
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: 'password')
    verify(pipeline).sh(shArgs.capture())
    assertThat(shArgs.value.script, containsString('--context=\'context\''))
  }

  @Test void register_docker_registry_does_not_use_context_if_it_null() {
    client.useContext()
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: 'password')
    verify(pipeline).sh(shArgs.capture())
    assertThat(shArgs.value.script, not(containsString('--context=')))
  }

  @Test void register_docker_registry_does_not_use_context_if_it_empty() {
    client.useContext('')
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: 'password')
    verify(pipeline).sh(shArgs.capture())
    assertThat(shArgs.value.script, not(containsString('--context=')))
  }

  @Test void register_docker_registry_does_not_use_context_if_it_whitespace() {
    client.useContext(' \t  ')
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: 'password')
    verify(pipeline).sh(shArgs.capture())
    assertThat(shArgs.value.script, not(containsString('--context=')))
  }

  @Test void returns_empty_context_when_it_is_set_to_null() {
    client.context = null
    assertThat(client.context, is(''))
    client.useContext()
    assertThat(client.context, is(''))
  }

  @Test void returns_empty_context_when_it_set_to_empty() {
    client.context = ''
    assertThat(client.context, is(''))
    client.useContext('')
    assertThat(client.context, is(''))
  }

  @Test void returns_empty_context_when_it_set_to_whitespaces() {
    client.context = ' \t  '
    assertThat(client.context, is(''))
    client.useContext(' \t  ')
    assertThat(client.context, is(''))
  }

  @Test void register_docker_registry_requires_secret_name_to_not_be_null() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Name of the secret is null')
    client.createDockerRegistrySecret(
      server: 'server', user: 'user', password: 'password')
  }

  @Test void register_docker_registry_requires_secret_name_to_not_be_empty() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Name of the secret is empty')
    client.createDockerRegistrySecret(name: '',
      server: 'server', user: 'user', password: 'password')
  }

  @Test void register_docker_registry_requires_secret_name_to_not_be_whitespace() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Name of the secret is whitespace')
    client.createDockerRegistrySecret(name: ' \t  ',
      server: 'server', user: 'user', password: 'password')
  }

  @Test void register_docker_registry_requires_server_option_to_not_be_null() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Docker registry server is null')
    client.createDockerRegistrySecret(name: 'secret',
      user: 'user', password: 'password')
  }

  @Test void register_docker_registry_requires_server_option_to_not_be_empty() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Docker registry server is empty')
    client.createDockerRegistrySecret(name: 'secret',
      server: '', user: 'user', password: 'password')
  }

  @Test void register_docker_registry_requires_server_option_to_not_be_whitespace() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Docker registry server is whitespace')
    client.createDockerRegistrySecret(name: 'secret',
      server: ' \t ', user: 'user', password: 'password')
  }

  @Test void register_docker_registry_requires_user_option_to_not_be_null() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Docker registry user is null')
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', password: 'password')
  }

  @Test void register_docker_registry_requires_user_option_to_not_be_empty() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Docker registry user is empty')
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: '', password: 'password')
  }

  @Test void register_docker_registry_requires_user_option_to_not_be_whitespace() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Docker registry user is whitespace')
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: ' \t ', password: 'password')
  }

  @Test void register_docker_registry_requires_password_option_to_not_be_null() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Docker registry password is null')
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user')
  }

  @Test void register_docker_registry_requires_password_option_to_not_be_empty() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Docker registry password is empty')
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: '')
  }

  @Test void register_docker_registry_requires_password_option_to_not_be_whitespace() {
    thrown.expect(IllegalArgumentException.class)
    thrown.expectMessage('Docker registry password is whitespace')
    client.createDockerRegistrySecret(name: 'secret',
      server: 'server', user: 'user', password: ' \t ')
  }
}

class K8sClientCommandHeaderTest {
  final K8sClient client = mock(K8sClient, CALLS_REAL_METHODS)

  @Test void has_only_keyword_by_default() {
    assertThat(client.commandHeader, is(equalTo('kubectl')))
  }

  @Test void has_kubeconfig_option_when_config_path_is_set() {
    client.configPath = 'config path'
    assertThat(client.commandHeader, is(equalTo('kubectl --kubeconfig=\'config path\'')))
  }

  @Test void has_context_option_when_context_is_set() {
    client.context = 'context'
    assertThat(client.commandHeader, is(equalTo('kubectl --context=\'context\'')))
  }

  @Test void does_not_contain_kubeconfig_option_when_config_path_is_null_or_empty_or_whitespaces() {
    [null, '', ' ', '\t', '\n', ' \t \n '].each { path ->
      client.configPath = path
      assertThat("\n     For: \'${ path }\'",
        client.commandHeader, not(containsString('--kubeconfig')))
    }
  }

  @Test void does_not_contain_context_option_when_context_is_null_or_empty_or_whitespaces() {
    [null, '', ' ', '\t', '\n', ' \t \n '].each { context ->
      client.context = context
      assertThat("\n     For: \'${ context }\'",
        client.commandHeader, not(containsString('--context')))
    }
  }
}
