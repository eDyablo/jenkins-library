package com.e4d.nuget

import com.e4d.pipeline.DummyPipeline
import org.junit.*
import static com.e4d.mockito.Matchers.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*

class NugetConfigTest {
  final config = new NugetConfig()
  final pipeline = mock(DummyPipeline)

  @Test void config_dir_contains_the_class_name() {
    assertThat(config.configDir, containsString(NugetConfig.name))
  }

  @Test void newly_created_has_config_path_equal_to_config_dir() {
    assertThat(config.configPath, is(equalTo(config.configDir)))
  }

  @Test void has_config_path_ends_with_reference_name_and_key() {
    config.configRef.with {
      name = 'name'
      key = 'key'
    }
    assertThat(config.configPath, endsWith('/name/key'))
  }

  @Test void defines_secret_volume_mapped_to_directory_named_as_secret_under_cofig_dir() {
    config.configRef.with {
      name = 'secret'
      key = 'key'
    }
    config.defineVolumes(pipeline)
    verify(pipeline).secretVolume(mapContains(
      secretName: 'secret',
      mountPath: "${ config.configDir }/secret"
    ))
  }

  @Test void defines_env_var_nuget_config_file_equals_to_its_config_path() {
    config.configRef.with {
      name = 'dir'
      key = 'file'
    }
    config.defineEnvVars(pipeline)
    verify(pipeline).envVar(mapContains(
      key: NugetConfig.CONFIG_FILE_ENV_VAR,
      value: config.configPath,
    ))
  }
}
