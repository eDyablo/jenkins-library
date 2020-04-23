package com.e4d.job

import com.e4d.pipeline.DummyPipeline
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class IntegrateNugetPackageJobDeclarationTest {
  final pipeline = spy(DummyPipeline)
  final job = spy(new IntegrateNugetPackageJob(pipeline))

  @Test void source_git_sets_job_git_source_reference() {
    job.declare {
      source {
        git 'repository'
      }
    }
    assertThat(job.gitSourceRef, allOf(
      is(not(null)),
      hasProperty('repository', equalTo('repository')),
    ))
  }

  @Test void source_git_sets_job_git_source_reference_from_map() {
    job.declare {
      source {
        git repository: 'repository',
          owner: 'owner',
          branch: 'branch',
          directory: 'directory'
      }
    }
    assertThat(job.gitSourceRef, allOf(
      is(not(null)),
      hasProperty('repository', equalTo('repository')),
      hasProperty('owner', equalTo('owner')),
      hasProperty('branch', equalTo('branch')),
      hasProperty('directory', equalTo('directory')),
    ))
  }

  @Test void source_git_sets_job_git_soource_reference_from_code() {
    job.declare {
      source {
        git {
          repository = 'repository'
          owner = 'owner'
          branch = 'branch'
          directory = 'directory'
        }
      }
    }
    assertThat(job.gitSourceRef, allOf(
      is(not(null)),
      hasProperty('repository', equalTo('repository')),
      hasProperty('owner', equalTo('owner')),
      hasProperty('branch', equalTo('branch')),
      hasProperty('directory', equalTo('directory')),
    ))
  }

  @Test void skip_prerelease_sets_job_publish_prerelease_option_to_false() {
    [
      {
        publishStrategy.skipPrereleaseVersion
      },

      {
        publish {
          strategy.skipPrereleaseVersion
        }
      },

      {
        publish {
          strategy {
            skipPrereleaseVersion
          }
        }
      },

      {
        publishStrategy {
          skipPrereleaseVersion
        }
      }
    ].each {
      job.publishPrereleaseVersion = true
      job.declare(it)
      assertThat(job.publishPrereleaseVersion, is(false))
    }
  }

  @Test void testing_project_file_pattern_sets_job_test_project_file_pattern() {
    [
      {
        testing {
          projectFilePattern 'pattern'
        }
      },

      {
        testing {
          projectFilePattern = 'pattern'
        }
      },

      {
        testing.projectFilePattern 'pattern'
      },

      {
        testing.projectFilePattern = 'pattern'
      },
    ].each {
      job.declare(it)
      assertThat(job.testProjectFilePattern, is(equalTo('pattern')))
    }
  }
}
