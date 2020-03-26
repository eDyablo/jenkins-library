package com.e4d.build

import org.junit.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class PublishNugetDeclarationTest {
  final List<Deployer> deployers = []
  PublishNugetDeclaration nuget

  @Before void beforeEach() {
    nuget = new PublishNugetDeclaration(deployers)
  }

  @Test void csproject_adds_one_proper_publisher() {
    nuget.csproject('project')
    assertThat(deployers, allOf(
      hasSize(equalTo(1)),
      contains(allOf(
        is(instanceOf(CSProjectNugetPublisher.class)),
        hasProperty('project', equalTo('project'))
      ))
    ))
  }

  @Test void two_csproject_calls_add_two_publishers() {
    nuget.csproject('project 1')
    nuget.csproject('project 2')
    assertThat(deployers, hasSize(equalTo(2)))
  }

  @Test void nupackage_adds_one_proper_publisher() {
    nuget.nupackage('package')
    assertThat(deployers, allOf(
      hasSize(equalTo(1)),
      contains(allOf(
        is(instanceOf(NuPackageNugetPublisher.class)),
        hasProperty('pkg', equalTo('package'))
      ))
    ))
  }

  @Test void csproject_set_version_when_specified() {
    nuget.csproject('project', version: 'version')
    assertThat(deployers, contains(
      hasProperty('version', equalTo('version'))
    ))
  }

  @Test void nupackage_set_version_prefix_when_specified() {
    nuget.csproject('package', versionPrefix: 'prefix')
    assertThat(deployers, contains(
      hasProperty('versionPrefix', equalTo('prefix'))
    ))
  }

  @Test void nupackage_set_version_suffix_when_specified() {
    nuget.csproject('package', versionSuffix: 'suffix')
    assertThat(deployers, contains(
      hasProperty('versionSuffix', equalTo('suffix'))
    ))
  }

  @Test void two_nupackage_calls_add_two_publishers() {
    nuget.nupackage('package 1')
    nuget.nupackage('package 2')
    assertThat(deployers, hasSize(equalTo(2)))
  }
}
