package com.e4d.job

import com.e4d.ioc.Context
import com.e4d.ioc.ContextRegistry
import com.e4d.pip.PipConfig
import com.e4d.pipeline.DummyPipeline
import com.e4d.shell.Shell
import org.junit.*
import org.mockito.ArgumentCaptor
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class IntegratePythonPackageJobNewlyCreatedTest {
  final pipConfig = mock(PipConfig)
  final pipeline = mock(DummyPipeline)
  final shell = mock(Shell)
  final job = new IntegratePythonPackageJob(pipeline, shell)

  @Test void has_default_git_config() {
    final gitDefault = [:]
    gitDefault.with(DefaultValues.git)
    assertThat(job.gitConfig, allOf(
      hasProperty('baseUrl', equalTo(gitDefault.baseUrl)),
      hasProperty('branch', equalTo(gitDefault.branch)),
      hasProperty('credsId', equalTo(gitDefault.credsId)),
    ))
  }

  @Test void has_default_pip_config() {
    final pipDefault = [:]
    pipDefault.with(DefaultValues.pip)
    assertThat(job.pipConfig, allOf(
      hasProperty('configRef', equalTo(pipDefault.configRef)),
    ))
  }

  @Test void has_default_nexus_config() {
    final nexusDefault = [:]
    nexusDefault.with(DefaultValues.nexus)
    assertThat(job.nexusConfig, allOf(
      hasProperty('baseUrl', equalTo(nexusDefault.baseUrl)),
      hasProperty('port', equalTo(nexusDefault.port)),
      hasProperty('credsId', equalTo(nexusDefault.credsId)),
      hasProperty('apiKey', equalTo(nexusDefault.apiKey)),
    ))
  }

  @Test void has_empty_source_root() {
    assertThat(job.sourceRoot, is(''))
  }
}

class IntegratePythonPackageJobTest {
  final context = mock(Context)
  final pipConfig = mock(PipConfig)
  final pipeline = mock(DummyPipeline, CALLS_REAL_METHODS)
  final shell = mock(Shell)
  final job = new IntegratePythonPackageJob(pipeline, shell)
  final options = ArgumentCaptor.forClass(Map)
  final shellArgs = ArgumentCaptor.forClass(Map)

  @Before void beforeEachTest() {
    job.pipConfig = pipConfig
    when(context.pipeline).thenReturn(pipeline)
    when(pipeline.checkout(any(Map))).thenReturn([:])
    when(pipeline.sh(any(Map))).thenReturn('{}')
    ContextRegistry.registerContext(context)
  }

  @Test void checks_out_from_url_and_repository_specified_in_job_git_config() {
    job.gitConfig.baseUrl = 'url'
    job.gitConfig.repository = 'repository'
    job.run()
    verify(pipeline).checkout(options.capture())
    assertThat(options.value.userRemoteConfigs[0].url.toString(),
      is('https://url/repository'))
  }

  @Test void checks_out_from_branch_specified_in_job_git_config() {
    job.gitConfig.branch = 'branch'
    job.run()
    verify(pipeline).checkout(options.capture())
    assertThat(options.value.branches.first(), is([name: 'branch']))
  }

  @Test void checks_out_from_pull_request_branch_when_it_is_in_parameters() {
    when(pipeline.params).thenReturn(sha1: 'pull request')
    job.gitConfig.branch = 'branch'
    job.run()
    verify(pipeline).checkout(options.capture())
    assertThat(options.value.branches.first(), is([name: 'pull request']))
  }

  @Test void checks_out_using_creds_from_job_git_config() {
    job.gitConfig.credsId = 'creds'
    job.run()
    verify(pipeline).checkout(options.capture())
    assertThat(options.value.userRemoteConfigs[0].credentialsId,
      is('creds'))
  }

  @Test void uses_source_root_as_working_directory() {
    job.sourceRoot = 'source root'
    job.run()
    verify(pipeline).dir(eq('source root'), any())
  }

  @Test void defines_env_vars_for_its_pip_config_with_its_pipeline() {
    when(pipConfig.defineEnvVars(pipeline)).thenReturn(['pip var 1', 'pip var 2'])
    assertThat(job.defineEnvVars(), is(equalTo(['pip var 1', 'pip var 2'])))
  }

  @Test void defines_volume_for_its_pip_config_with_its_pipeline() {
    when(pipConfig.defineVolumes(pipeline)).thenReturn(['pip volume 1', 'pip volume 2'])
    assertThat(job.defineVolumes(), is(equalTo(['pip volume 1', 'pip volume 2'])))
  }
}
