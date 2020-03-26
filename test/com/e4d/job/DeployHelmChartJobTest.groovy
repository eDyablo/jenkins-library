package com.e4d.job

import com.e4d.helm.HelmTool
import com.e4d.k8s.K8sConfig
import com.e4d.pipeline.DummyPipeline
import com.e4d.secret.PipelineSecret
import java.net.URI
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class DeployHelmChartJobTest {
  final pipeline = spy(new DummyPipeline())
  final helm = mock(HelmTool)
  final job = spy(new DeployHelmChartJob(pipeline,
    helm: helm))

  @Test void when_created_has_intialized_helm() {
    final job = new DeployHelmChartJob(pipeline)
    assertThat(job.helm, allOf(
      is(not(null)),
      hasProperty('shell', allOf(
        is(not(null)),
        hasProperty('pipeline', is(sameInstance(pipeline)))
      )),
    ))
  }

  @Test void has_initialized_helm_when_created() {
    final job = new DeployHelmChartJob(pipeline)
    assertThat(job.helm, allOf(
      is(not(null)),
      hasProperty('shell', allOf(
        is(not(null)),
        hasProperty('pipeline', is(sameInstance(pipeline)))
      )),
    ))
  }

  @Test void when_created_has_default_nexus_config() {
    final nexus = [:]
    nexus.with(DefaultValues.nexus)
    final job = new DeployHelmChartJob(pipeline)
    assertThat(job.nexusConfig, allOf(
      hasProperty('baseUrl', equalTo(nexus.baseUrl)),
      hasProperty('port', equalTo(nexus.port)),
      hasProperty('credsId', equalTo(nexus.credsId)),
      hasProperty('apiKey', equalTo(nexus.apiKey)),
    ))
  }

  @Test void has_destination_when_created() {
    final job = new DeployHelmChartJob(pipeline)
    assertThat(job.destination, is(not(null)))
  }

  @Test void run_fetches_chart_in_fetch_stage() {
    doReturn([:]).when(helm).fetchChart(argThat(any(Map)))
    job.run()
    verify(pipeline).stage(argThat(equalTo('fetch')), argThat(any(Closure)))
    verify(job).fetchChart()
  }

  @Test void fetch_chart_fetches_chart_via_helm_by_specifying_chart_url() {
    doReturn(new URI('chart')).when(job).chart
    job.fetchChart()
    verify(helm).fetchChart(argThat(hasEntry('chartURL', 'chart')))
  }

  @Test void chart_returns_uri_starts_with_chart_repository_uri_when_it_is_set_from_relative_uri() {
    [
      [                   '',      '', ''],
      [                   '', 'chart', 'chart'],
      [                  '/',      '', '/'],
      [                  '/', 'chart', '/chart'],
      [              'root/',      '', 'root/'],
      [              'root/', 'chart', 'root/chart'],
      [         'root/base/',      '', 'root/base/'],
      [         'root/base/', 'chart', 'root/base/chart'],
      [     'scheme://host/',      '', 'scheme://host/'],
      [     'scheme://host/', 'chart', 'scheme://host/chart'],
      ['scheme://host/repo/',      '', 'scheme://host/repo/'],
    ].each { repo, chart, expected ->
      doReturn(new URI(repo)).when(job).chartRepositoryURI
      job.chart = new URI(chart)
      assertThat("\n     For: '${repo}' and '${chart}'",
        job.chart.toString(), equalTo(expected))
    }
  }

  @Test void chat_returns_uri_does_not_start_from_repository_uri_when_it_is_set_from_absolute_uri() {
    doReturn(new URI('scheme://host/repository')).when(job).chartRepositoryURI
    [
      'scheme://chart-host',
      'scheme://chart-host/chart-repo',
    ].each { chart ->
      job.chart = new URI(chart)
      assertThat("\n     For: '${ chart }'",
        job.chart.toString(), allOf(
          not(equalTo('scheme://host/repository')),
          equalTo(chart)))
    }
  }

  @Test void chart_repository_uri_returns_uri_built_from_nexus_config_base_url() {
    [
      [      '', new URI('repository/charts/')],
      [ 'nexus', new URI('nexus/repository/charts/')],
      ['nexus/', new URI('nexus/repository/charts/')],
    ].each { nexusURL, expected ->
      job.nexusConfig.baseUrl = nexusURL
      assertThat("\n     For: '${ nexusURL }'",
        job.chartRepositoryURI, is(equalTo(expected)))
    }
  }

  @Test void chart_repository_uri_has_user_info_got_from_nexus_config_credentials() {
    job.nexusConfig.credsId = 'nexus creds'
    doReturn(['nexus user', 'nexus password']).when(job).getUsernamePassword('nexus creds')
    assertThat(job.chartRepositoryURI,
      hasProperty('userInfo', equalTo('nexus user:nexus password'))
    )
  }

  @Test void chart_returns_uri_contains_user_info_from_chart_repository_uri() {
    final repositoryURI = new URI('')
    repositoryURI.userInfo = 'user info'
    doReturn(repositoryURI).when(job).chartRepositoryURI
    job.chartURI = new URI('')
    assertThat(job.chart,
      hasProperty('userInfo', equalTo('user info'))
    )
  }

  @Test void fetch_chart_downloads_file_using_user_and_password_from_user_info_of_chart_uri() {
    [
      ['user:password', 'user', 'password'],
      [         'user', 'user', null],
      [             '',   null, null],
    ].each { userInfo, user, password ->
      job.chartURI = new URI('')
      job.chartURI.userInfo = userInfo
      job.fetchChart()
      verify(helm).fetchChart(argThat(allOf(
        hasEntry('user', user),
        hasEntry('password', password),
      )))
      reset(helm)
    }
  }

  @Test void chart_returns_chart_repository_uri_when_no_chart_uri_set() {
    job.chartURI = null
    assertThat(job.chart, is(equalTo(job.chartRepositoryURI)))
  }

  @Test void chart_repository_uri_has_null_user_info_when_user_and_password_are_null_or_empty() {
    [
      [null, null],
      ['', ''],
      [null, ''],
      ['', null],
    ].each { user, password ->
      doReturn([user, password]).when(job).getUsernamePassword(argThat(any(String)))
      assertThat("\n     For: '${ user }' and '${ password }'",
        job.chartRepositoryURI, hasProperty('userInfo', equalTo(null)))
    }
  }

  @Test void chart_repository_uri_has_user_info_equal_to_user_when_password_is_null_or_empty() {
    [ null, ''].each { password ->
      doReturn(['user', password]).when(job).getUsernamePassword(argThat(any(String)))
      assertThat("\n     For: '${ password }'",
        job.chartRepositoryURI, hasProperty('userInfo', equalTo('user')))
    }
  }

  @Test void chart_returns_uri_with_user_info_from_chart_repostitory_uri() {
    final repositoryURI = new URI()
    repositoryURI.userInfo = 'user info'
    doReturn(repositoryURI).when(job).chartRepositoryURI
    assertThat(job.chart, hasProperty('userInfo', equalTo('user info')))
  }

  @Test void chart_returns_uri_with_user_info_from_chart_uri_when_it_is_set() {
    final repositoryURI = new URI()
    repositoryURI.userInfo = 'repository user info'
    doReturn(repositoryURI).when(job).chartRepositoryURI
    job.chartURI = new URI()
    job.chartURI.userInfo = 'chart user info'
    assertThat(job.chart, hasProperty('userInfo', equalTo('chart user info')))
  }

  @Test void chart_repository_uri_does_not_expose_user_info_in_its_textual_representation() {
    doReturn(['user', 'password']).when(job).getUsernamePassword(argThat(any(String)))
    job.nexusConfig.baseUrl = 'scheme://host/path'
    assertThat(job.chartRepositoryURI, hasToString(not(containsString('user:password'))))
  }

  @Test void chart_does_not_expose_user_info_in_its_textual_representation() {
    final repository = new URI('scheme://host/repository')
    repository.userInfo = 'user:info'
    doReturn(repository).when(job).chartRepositoryURI
    assertThat(job.chart, hasToString(not(containsString('user:info'))))
  }

  @Test void fetch_chart_sets_fetched_chart() {
    doReturn('fetched chart').when(helm).fetchChart(argThat(any(Map)))
    job.fetchChart()
    assertThat(job.fetchedChart, is(equalTo('fetched chart')))
  }

  @Test void run_deploys_chart_in_deploy_stage() {
    doReturn([:]).when(helm).fetchChart(argThat(any(Map)))
    job.run()
    verify(pipeline).stage(argThat(equalTo('deploy')), argThat(any(Closure)))
    verify(job).deployChart()
  }

  @Test void has_default_k8s_config_when_created() {
    final values = [:]
    values.with(DefaultValues.k8s)
    assertThat(job.k8sConfig, allOf(
      hasProperty('configRef', equalTo(values.configRef)),
    ))
  }

  @Test void defines_volumes_for_its_k8s_config() {
    final k8sConfig = mock(K8sConfig)
    doReturn('k8s volume').when(k8sConfig).defineVolumes(pipeline)
    job.k8sConfig = k8sConfig
    assertThat(job.defineVolumes(), is(equalTo('k8s volume')))
  }

  @Test void deploys_fetched_chart_by_upgrading_chart_release_with_the_chart_name_and_path() {
    job.fetchedChart = [
      name: 'fetched chart',
      path: 'fetched chart path',
    ]
    job.deployChart()
    verify(helm).upgradeChartRelease(
      argThat(any(Map)),
      argThat(equalTo('fetched chart')),
      argThat(equalTo('fetched chart path'))
    )
  }

  @Test void deploys_fetched_chart_uses_kube_config_from_job_k8s_config() {
    final k8sConfig = mock(K8sConfig)
    doReturn('k8s config').when(k8sConfig).configPath
    job.k8sConfig = k8sConfig
    job.fetchedChart = [name: 'name', path: 'path']
    job.deployChart()
    verify(helm).upgradeChartRelease(
      argThat(hasEntry('kubeConfig', 'k8s config')),
      argThat(any(String)),
      argThat(any(String))
    )
  }

  @Test void deploys_fetched_chart_to_kube_context_and_namespace_set_in_job_deploy_destination() {
    job.fetchedChart = [name: 'name', path: 'path']
    job.destination = new DeployDestination(
      context: 'k8s context', namespace: 'k8s namespace')
    job.deployChart()
    verify(helm).upgradeChartRelease(
      argThat(allOf(
        hasEntry('kubeContext', 'k8s context'),
        hasEntry('namespace', 'k8s namespace'),
      )),
      argThat(any(String)),
      argThat(any(String))
    )
  }

  @Test void allows_to_set_destination_from_text() {
    job.destination = 'context : namespace'
    assertThat(job.destination, allOf(
      hasProperty('context', equalTo('context')),
      hasProperty('namespace', equalTo('namespace')),
    ))
  }

  @Test void deploy_fetched_chart_requests_install() {
    job.fetchedChart = [name: 'name', path: 'path']
    job.deployChart()
    verify(helm).upgradeChartRelease(
      argThat(hasEntry('install', true)),
      argThat(any(String)),
      argThat(any(String))
    )
  }

  @Test void composes_release_name_from_fetched_chart_name_and_destination_namespace() {
    [
      ['namespace', 'chart', 'namespace-chart'],
      [       null, 'chart', 'chart'],
      [         '', 'chart', 'chart'],
      [        ' ', 'chart', 'chart'],
      [       '\t', 'chart', 'chart'],
      [       '\n', 'chart', 'chart'],
      [  ' \t \n ', 'chart', 'chart'],
    ].each { namespace, chart, release ->
      job.fetchedChart = [name: chart]
      job.destination = new DeployDestination(namespace: namespace)
      assertThat("\n     For: '${ namespace }' and '${ chart }'",
        job.releaseName, is(equalTo(release)))
    }
  }

  @Test void does_not_include_default_namespace_into_release_name() {
    job.fetchedChart = [name: 'chart']
    job.destination = new DeployDestination(namespace: 'default')
    assertThat(job.releaseName, is(equalTo('chart')))
  }

  @Test void deploys_fetched_chart_to_release_defined_by_job_release_name() {
    job.fetchedChart = [:]
    doReturn('release name').when(job).releaseName
    job.deployChart()
    verify(helm).upgradeChartRelease(
      argThat(any(Map)),
      argThat(equalTo('release name')),
      any()
    )
  }

  @Test void deploys_fetched_chart_with_value_files_resolved_agains_fetched_chart_path() {
    job.fetchedChart = [path: 'chart']
    job.valueFiles = ['first', 'second']
    job.deployChart()
    verify(helm).upgradeChartRelease(
      argThat(
        hasEntry(
          equalTo('valueFiles'),
          contains('chart/first', 'chart/second')
        )
      ),
      any(),
      any()
    )
  }

  @Test void fetch_chart_upacks_the_chart() {
    job.fetchChart()
    verify(helm).fetchChart(
      argThat(
        hasEntry('unpack', true)
      )
    )
  }

  @Test void waits_when_deploys_fetched_chart() {
    job.fetchedChart = [:]
    job.deployChart()
    verify(helm).upgradeChartRelease(
      argThat(
        hasEntry('wait', true)
      ),
      any(),
      any()
    )
  }

  @Test void has_empty_chart_values_when_created() {
    final job = new DeployHelmChartJob(pipeline)
    assertThat(job.chartValues, is(equalTo([:])))
  }

  @Test void deploy_chart_writes_yaml_file_with_chart_values() {
    job.fetchedChart = [:]
    job.chartValues = [
      first: [
        value: 1,
      ],
      second: [
        value: 2,
      ],
    ]
    job.deployChart()
    verify(pipeline).writeYaml(argThat(
      allOf(
        hasEntry('file', 'values'),
        hasEntry('data', [
          first: [
            value: 1,
          ],
          second: [
            value: 2,
          ],
        ])
      )
    ))
  }

  @Test void deploy_chart_does_not_write_yaml_file_with_chart_values_when_they_are_not_defined() {
    job.fetchedChart = [:]
    job.chartValues = [:]
    job.deployChart()
    verify(pipeline, never()).writeYaml(argThat(any(Map)))
  }

  @Test void deploy_chart_upgrades_release_with_values_file_when_chart_values_are_defined() {
    job.fetchedChart = [:]
    job.chartValues = [value: 1]
    job.deployChart()
    verify(helm).upgradeChartRelease(
      argThat(hasEntry(
        equalTo('valueFiles'),
        contains('values')
      )),
      any(),
      any()
    )
  }

  @Test void load_parameters_sets_chart_with_helm_chart_parameter_when_defined() {
    job.loadParameters([helmChart: 'helm-chart'])
    assertThat(job.chartURI, is(equalTo(new URI('helm-chart'))))
  }

  @Test void load_parameters_does_not_set_chart_when_helm_chart_parameter_is_not_defined() {
    job.chartURI = new URI('chart')
    job.loadParameters([helmChart: null])
    assertThat(job.chartURI, is(equalTo(new URI('chart'))))
  }

  @Test void deploy_chart_declassifies_pipeline_secrets_in_chart_values() {
    final secret = mock(PipelineSecret)
    when(secret.declassify(pipeline)).thenReturn('declassified')
    job.fetchedChart = [:]
    job.chartValues = [
      level: 1,
      secret: secret,
      map: [
        level: 2,
        secret: secret,
      ],
      map: [
        level: 2,
        map: [
          level: 3,
          secret: secret,
        ]
      ],
    ]
    job.deployChart()
    verify(pipeline).writeYaml(argThat(
      hasEntry('data', [
        level: 1,
        secret: 'declassified',
        map: [
          level: 2,
          secret: 'declassified',
        ],
        map: [
          level: 2,
          map: [
            level: 3,
            secret: 'declassified',
          ]
        ],
      ])
    ))
  }
}
