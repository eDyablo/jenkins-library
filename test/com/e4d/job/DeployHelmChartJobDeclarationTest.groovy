package com.e4d.job

import com.e4d.pipeline.DummyPipeline
import com.e4d.secret.SecretTextBearer
import java.net.URI
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class DeployHelmChartJobDeclarationTest {
  final pipeline = mock(DummyPipeline)
  final job = spy(new DeployHelmChartJob(pipeline))

  @Test void chart_sets_job_chart() {
    job.declare {
      chart 'chart'
    }
    verify(job).setChart('chart')
  }

  @Test void destination_sets_job_destination() {
    job.declare {
      destination 'destination'
    }
    verify(job).setDestination('destination')
  }

  @Test void values_from_file_collects_files_into_job_value_files() {
    job.declare {
      values {
        fromFile 'first'
        fromFile 'second'
      }
      values {
        fromFile 'third'
      }
    }
    assertThat(job.valueFiles, contains('first', 'second', 'third'))
  }

  @Test void values_from_values_sets_chart_values_for_the_job() {
    job.declare {
      values {
        fromValues one: 1,
          two: 2
      }
      values {
        fromValues three: 3
      }
    }
    assertThat(job.chartValues, is(equalTo([one: 1, two: 2, three: 3])))
  }

  @Test void values_from_values_overrides_job_chart_values() {
    job.chartValues = [
      first: 1,
      second: 2,
      third: 3,
    ]
    job.declare {
      values {
        fromValues second: 'overriden'
      }
    }
    assertThat(job.chartValues, is(equalTo(
      [first: 1, second: 'overriden', third: 3])))
  }

  @Test void values_from_values_merges_specified_values_with_job_chart_values() {
    job.chartValues = [
      first: 1,
      second: 2,
    ]
    job.declare {
      values {
        fromValues third: 3
      }
      values {
        fromValues fourth: 4
      }
    }
    assertThat(job.chartValues, is(equalTo(
      [first: 1, second: 2, third: 3, fourth: 4])))
  }

  @Test void values_from_values_overrides_previously_specified_values() {
    job.declare {
      values {
        fromValues value: 1
        fromValues value: 2
      }
      values {
        fromValues value: 3
      }
    }
    assertThat(job.chartValues, is(equalTo([value: 3])))
  }

  @Test void from_values_merges_all_maps_into_one_job_chart_values() {
    job.declare {
      values {
        fromValues level: 0,
          altered: false,
          map: [
            level: 1,
            altered: false,
            map: [
              level: 2,
              altered: false,
            ]
          ]
      }
      values {
        fromValues altered: true,
          extra: 0,
          map: [
            altered: true,
            extra: 1,
            map: [
              altered: true,
              extra: 2,
            ]
          ]
      }
    }
    assertThat(job.chartValues, is(equalTo([
      level: 0,
      altered: true,
      extra: 0,
      map: [
        level: 1,
        extra: 1,
        altered: true,
        map: [
          level: 2,
          extra: 2,
          altered: true,
        ]
      ]
    ])))
  }
}

class DeployHelmChartJobValuesDeclarationTest {
  final declaration = spy(new DeployHelmChartJob.ValuesDeclaration(
    files: [], values: [:]))

  @Test void values_declaration_from_redirects_to_from_file_when_argument_is_a_string() {
    declaration.from('text')
    verify(declaration).fromFile('text')
  }

  @Test void values_declaration_from_redirects_to_from_values_when_argument_is_a_map() {
    declaration.from([value: 1])
    verify(declaration).fromValues([value: 1])
  }

  @Test void secret_text_returns_secret_text_bearer() {
    assertThat(declaration.secretText('secret'), allOf(
      is(not(null)),
      is(instanceOf(SecretTextBearer)),
      hasProperty('secretId', equalTo('secret'))
    ))
  }
}
