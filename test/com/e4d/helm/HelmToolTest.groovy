package com.e4d.helm

import com.e4d.shell.Shell
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class HelmToolTest {
  final shell = mock(Shell)
  final helm = spy(new HelmTool(shell))

  @Test void package_chart_in_chart_path() {
    helm.packageChart(chartPath: 'chart path')
    final scriptMatch = stringContainsInOrder(
      'helm', 'package', "'chart path'")
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), scriptMatch)), eq([]))
  }

  @Test void package_chart_uses_current_directory_when_no_chart_path_is_defined() {
    [
      null, '', ' ',
      '\t', '\n',
    ].each { path ->
      helm.packageChart(chartPath: path)
      final scriptMatch = stringContainsInOrder(
        'helm', 'package', " .")
      verify(shell).execute(
        argThat(hasEntry(equalTo('script'), scriptMatch)), eq([]))
      reset(shell)
    }
  }

  @Test void package_chart_uses_current_directory_when_no_option_is_set() {
    helm.packageChart()
    final scriptMatch = stringContainsInOrder(
      'helm', 'package', " .")
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), scriptMatch)), eq([]))
  }

  @Test void package_chart_gets_stdout_of_executed_script() {
    helm.packageChart()
    verify(shell).execute(
      argThat(hasEntry(equalTo('returnStdout'), equalTo(true))), eq([]))
  }

  @Test void package_chart_returns_no_path_when_stdout_is_empty() {
    [
      null, '', ' ',
      '\t', '\n',
    ].each { stdout ->
      when(shell.execute(any(Map), any(List))).thenReturn(stdout)
      final pkg = helm.packageChart()
      assertThat(pkg, hasEntry('path', null))
    }
  }

  @Test void package_chart_returns_package_path_extracted_from_stdout() {
    when(shell.execute(any(Map), any(List)))
      .thenReturn('Successfully packaged chart and saved it to: /directory/chart')
    final pkg = helm.packageChart()
    assertThat(pkg, hasEntry('path', '/directory/chart'))
  }

  @Test void lint_chart_runs_lint_with_specified_path() {
    helm.lintChart(path: 'path')
    final scriptMatch = stringContainsInOrder(
      'helm', 'lint', "'path'")
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), scriptMatch)), eq([]))
  }

  @Test void lint_chart_uses_current_directory_when_no_path_is_defined() {
    [
      null, '', ' ',
      '\t', '\n',
    ].each { path ->
      helm.lintChart(path: path)
      final scriptMatch = stringContainsInOrder(
        'helm', 'lint', " .")
      verify(shell).execute(
        argThat(hasEntry(equalTo('script'), scriptMatch)), eq([]))
      reset(shell)
    }
  }

  @Test void lint_chart_uses_current_directory_when_no_option_is_set() {
    helm.lintChart()
    final scriptMatch = stringContainsInOrder(
      'helm', 'lint', " .")
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), scriptMatch)), eq([]))
  }

  @Test void lint_chart_gets_stdout_of_executed_script() {
    helm.lintChart()
    verify(shell).execute(
      argThat(hasEntry(equalTo('returnStdout'), equalTo(true))), eq([]))
  }

  @Test void lint_chart_returns_no_messages_when_stdout_is_empty() {
    [
      null, '', ' ',
      '\t', '\n',
    ].each { stdout ->
      when(shell.execute(any(Map), any(List))).thenReturn(stdout)
      final pkg = helm.lintChart()
      assertThat(pkg, is(equalTo([
        messages: [],
        warnings: [],
        errors: [],
      ])))
    }
  }

  @Test void lint_chart_returns_all_information_messages_from_stdout() {
    when(shell.execute(any(Map), any(List))).thenReturn('''\
      [INFO] one
      [INFO] two
      [INFO] three
    '''.stripIndent())
    final result = helm.lintChart()
    assertThat(result.messages, is(equalTo(['one', 'two', 'three'])))
  }

  @Test void lint_chart_returns_all_warnings_from_stdout() {
    when(shell.execute(any(Map), any(List))).thenReturn('''\
      [WARNING] first warning
      [WARNING] second warning
      [WARNING] third warning
    '''.stripIndent())
    final result = helm.lintChart()
    assertThat(result.warnings, is(equalTo([
      'first warning', 'second warning', 'third warning'])))
  }

  @Test void lint_chart_returns_all_errors_from_stdout() {
    when(shell.execute(any(Map), any(List))).thenReturn('''\
      [ERROR] first error
      [ERROR] second error
      [ERROR] third error
    '''.stripIndent())
    final result = helm.lintChart()
    assertThat(result.errors, is(equalTo([
      'first error', 'second error', 'third error'])))
  }

  @Test void package_chart_sets_version_when_it_is_specified() {
    helm.packageChart(version: 'version')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'), containsString('--version version'))), eq([]))
  }

  @Test void package_chart_does_not_set_version_when_it_is_not_specified() {
    [
      null, '', ' ',
      '\t', '\n',
    ].each { version ->
      helm.packageChart(version: version)
      verify(shell).execute(
        argThat(hasEntry(equalTo('script'), not(containsString('--version')))), eq([]))
      reset(shell)
    }
  }

  @Test void package_chart_returns_name_extracted_from_stdout() {
    when(shell.execute(any(Map), any(List))).thenReturn('name: chart')
    final pkg = helm.packageChart()
    assertThat(pkg, hasEntry('name', 'chart'))
  }

  @Test void package_chart_returns_no_name_when_stdout_is_empty() {
    [
      null, '', ' ',
      '\t', '\n',
    ].each { stdout ->
      when(shell.execute(any(Map), any(List))).thenReturn(stdout)
      final pkg = helm.packageChart()
      assertThat(pkg, hasEntry('name', null))
    }
  }

  @Test void fetch_chart_uses_specified_chart_url() {
    helm.fetchChart(chartURL: 'chart url')
    verify(shell).execute(
      argThat(hasEntry(equalTo('script'),
        stringContainsInOrder('fetch', "'chart url'"))),
      any(List)
    )
  }

  @Test void fetch_chart_uses_specified_user_via_envs() {
    helm.fetchChart(user: 'user')
    verify(shell).execute(
      argThat(hasEntry(
        equalTo('script'),
        stringContainsInOrder(
          'inspect', '--username', '"${user}"',
          'fetch', '--username', '"${user}"'
        )
      )),
      argThat(hasItem('user=user'))
    )
  }

  @Test void fetch_chart_does_not_set_user_when_it_is_not_specified() {
    [
      null, '', ' ',
    ].each { user ->
      helm.fetchChart(user: user)
      verify(shell).execute(
        argThat(hasEntry(equalTo('script'), allOf(
          not(containsString('--username')),
          not(containsString('"${user}"')),
        ))),
        argThat(not(hasItem('user=user')))
      )
      reset(shell)
    }
  }

  @Test void fetch_chart_uses_specified_password_via_envs() {
    helm.fetchChart(password: 'password')
    verify(shell).execute(
      argThat(hasEntry(
        equalTo('script'),
        stringContainsInOrder(
          'inspect', '--password', '"${password}"',
          'fetch', '--password', '"${password}"'
        )
      )),
      argThat(hasItem('password=password'))
    )
  }

  @Test void fetch_chart_does_not_set_password_when_it_is_not_specified() {
    [
      null, '', ' ',
    ].each { password ->
      helm.fetchChart(password: password)
      verify(shell).execute(
        argThat(hasEntry(equalTo('script'), allOf(
          not(containsString('--password')),
          not(containsString('"${password}"')),
        ))),
        argThat(not(hasItem('password=password')))
      )
      reset(shell)
    }
  }

  @Test void fetch_chart_inspects_the_chart_using_chart_url() {
    [
      'chart',
      'repo/chart',
    ].each { url ->
      helm.fetchChart(chartURL: url)
      verify(shell).execute(
        argThat(hasEntry(equalTo('script'),
          stringContainsInOrder('helm inspect chart', "'${ url }'")
        )),
        any(List)
      )
      reset(shell)
    }
  }

  @Test void fetch_chart_gets_the_stdoutput_of_its_shell_script() {
    helm.fetchChart([:])
    verify(shell).execute(argThat(hasEntry('returnStdout', true)), any(List))
  }

  @Test void fetch_chart_returns_valid_result_extracted_from_output() {
    [
      [
        '',
        [:]
      ],
      [
        'name: chart name',
        [name: 'chart name']
      ],
      [
        'description: chart description',
        [description: 'chart description']
      ],
      [
        'version: chart version',
        [version: 'chart version']
      ],
      [
        '''
        name: chart name
        description: chart description
        '''.stripIndent(),
        [
          name: 'chart name',
          description: 'chart description',
        ]
      ],
    ].each { output, expected ->
      doReturn(output).when(shell).execute(any(Map), any(List))
      final chart = helm.fetchChart([:])
      assertThat("\n     For: '${ output }'", chart, equalTo(expected))
    }
  }

  @Test void fetch_chart_result_has_path_refers_to_fetched_chart() {
    [
      [           'chart', 'chart'],
      ['repository/chart', 'chart'],
    ].each { chartURL, path ->
      final chart = helm.fetchChart(chartURL: chartURL)
      assertThat("\n     For: '${ chartURL }'",
        chart, hasEntry('path', path))
    }
  }

  @Test void fetch_chart_untar_the_chart_when_unpack_option_is_set() {
    helm.fetchChart(chartURL: 'chart', unpack: true)
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder('fetch', '--untar', 'chart')
        )
      ),
      any(List)
    )
  }

  @Test void fetch_chart_does_not_untar_by_default() {
    helm.fetchChart(chartURL: 'chart')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          not(containsString('--untar'))
        )
      ),
      any(List)
    )
  }

  @Test void fetch_chart_result_has_path_equal_to_the_chart_name_from_chart_info_when_unpack() {
    doReturn([name: 'chart name']).when(helm).extractChartInfo(any())
    final chart = helm.fetchChart(chartURL: 'chart url', unpack: true)
    assertThat(chart, hasEntry('path', 'chart name'))
  }

  @Test void upgrade_chart_release_upgrades_specified_release_with_specified_chart() {
    helm.upgradeChartRelease('release', 'chart')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder('helm upgrade', 'release', "'chart'")
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_uses_specified_kube_config() {
    helm.upgradeChartRelease('release', 'chart', kubeConfig: 'kube config')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString("--kubeconfig 'kube config'")
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_does_not_use_kube_config_when_it_is_not_specified() {
    helm.upgradeChartRelease('release', 'chart')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          not(containsString('--kubeconfig'))
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_uses_specified_kube_context() {
    helm.upgradeChartRelease('release', 'chart', kubeContext: 'kube context')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString("--kube-context 'kube context'")
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_does_not_use_kube_context_when_it_is_not_specified() {
    helm.upgradeChartRelease('release', 'chart')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          not(containsString('--kube-context'))
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_uses_specified_namespace() {
    helm.upgradeChartRelease('release', 'chart', namespace: 'namespace')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString('--namespace namespace')
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_does_not_use_namespace_when_it_is_not_specified() {
    helm.upgradeChartRelease('release', 'chart')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          not(containsString('--namespace'))
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_runs_install_when_requested() {
    helm.upgradeChartRelease('release', 'chart', install: true)
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString('--install')
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_does_not_run_install_when_it_is_not_requested() {
    helm.upgradeChartRelease('release', 'chart', install: false)
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          not(containsString('--install'))
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_uses_specified_value_files() {
    helm.upgradeChartRelease('release', 'chart', valueFiles: ['first', 'second'])
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder("--values 'first'", "--values 'second'")
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_does_not_use_null_or_empty_entries_from_value_files() {
    helm.upgradeChartRelease('release', 'chart', valueFiles: [null, ''])
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          not(containsString('--values'))
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_waits_for_ready_state_when_specified() {
    helm.upgradeChartRelease('release', 'chart', wait: true)
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder('upgrade', '--wait')
        )
      ),
      any(List)
    )
  }

  @Test void upgrade_chart_release_does_not_wait_for_ready_state_by_default() {
    helm.upgradeChartRelease('release', 'chart')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          not(containsString('--wait'))
        )
      ),
      any(List)
    )
  }

  @Test void package_chart_updates_dependencies_when_requested() {
    helm.packageChart(updateDependencies: true)
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder('package', '--dependency-update')
        )
      ),
      any(List)
    )
  }

  @Test void package_chart_does_not_update_dependencies_when_not_requested() {
    helm.packageChart(updateDependencies: false)
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          not(containsString('--dependency-update'))
        )
      ),
      any(List)
    )
  }
}
