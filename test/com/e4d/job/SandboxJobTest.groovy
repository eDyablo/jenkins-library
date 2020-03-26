package com.e4d.job

import com.e4d.k8s.K8sConfig
import com.e4d.nuget.NugetConfig
import com.e4d.pip.PipConfig
import com.e4d.pipeline.DummyPipeline
import org.junit.*
import static com.e4d.mockito.Matchers.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*

class SandboxJobTest {
  final k8sConfig = mock(K8sConfig)
  final nugetConfig = mock(NugetConfig)
  final pipConfig = mock(PipConfig)
  final pipeline = mock(DummyPipeline)
  final job = new SandboxJob(pipeline)

  @Before void beforeEachTest() {
    job.k8sConfig = k8sConfig
    job.nugetConfig = nugetConfig
    job.pipConfig = pipConfig
  }

  @Test void initialize_job_sets_config_path_for_k8s_client_equal_to_path_from_jobs_k8s_config() {
    when(k8sConfig.configPath).thenReturn('config path')
    job.initializeJob()
    assertThat(job.k8sClient.configPath, is(equalTo('config path')))
  }

  @Test void defines_env_vars_for_its_k8s_config_with_its_pipeline() {
    when(k8sConfig.defineEnvVars(pipeline)).thenReturn(['k8s var 1', 'k8s var 2'])
    assertThat(job.defineEnvVars(), is(equalTo(['k8s var 1', 'k8s var 2'])))
  }

  @Test void defines_env_vars_for_its_pip_config_with_its_pipeline() {
    when(pipConfig.defineEnvVars(pipeline)).thenReturn(['pip var 1', 'pip var 2'])
    assertThat(job.defineEnvVars(), is(equalTo(['pip var 1', 'pip var 2'])))
  }

  @Test void defines_env_vars_for_its_nuget_config_with_its_pipeline() {
    when(nugetConfig.defineEnvVars(pipeline)).thenReturn(['nuget var 1', 'nuget var 2'])
    assertThat(job.defineEnvVars(), is(equalTo(['nuget var 1', 'nuget var 2'])))
  }

  @Test void defines_volume_for_its_k8s_config_with_its_pipeline() {
    when(k8sConfig.defineVolumes(pipeline)).thenReturn(['k8s volume 1', 'k8s volume 2'])
    assertThat(job.defineVolumes(), is(equalTo(['k8s volume 1', 'k8s volume 2'])))
  }

  @Test void defines_volume_for_its_pip_config_with_its_pipeline() {
    when(pipConfig.defineVolumes(pipeline)).thenReturn(['pip volume 1', 'pip volume 2'])
    assertThat(job.defineVolumes(), is(equalTo(['pip volume 1', 'pip volume 2'])))
  }

  @Test void defines_volume_for_its_nuget_config_with_its_pipeline() {
    when(nugetConfig.defineVolumes(pipeline)).thenReturn(['nuget volume 1', 'nuget volume 2'])
    assertThat(job.defineVolumes(), is(equalTo(['nuget volume 1', 'nuget volume 2'])))
  }
}

class SanboxJobNewlyCreatedTest {
  final job = new SandboxJob(mock(DummyPipeline))

  @Test void has_default_k8s_config_reference() {
    final defaultConfig = new K8sConfig()
    defaultConfig.with(DefaultValues.k8s)
    assertThat(job.k8sConfig.configRef,
      is(equalTo(defaultConfig.configRef)))
  }

  @Test void has_default_pip_config_reference() {
    final defaultConfig = new PipConfig()
    defaultConfig.with(DefaultValues.pip)
    assertThat(job.pipConfig.configRef,
      is(equalTo(defaultConfig.configRef)))
  }

  @Test void has_default_nuget_config_reference() {
    final defaultConfig = new PipConfig()
    defaultConfig.with(DefaultValues.nuget)
    assertThat(job.nugetConfig.configRef,
      is(equalTo(defaultConfig.configRef)))
  }
}
