package com.e4d.job

import com.e4d.docker.DockerTool
import com.e4d.file.FindTool
import com.e4d.git.GitConfig
import com.e4d.git.GitSourceReference
import com.e4d.nuget.NugetConfig
import com.e4d.nuget.NugetRepository
import com.e4d.pipeline.DummyPipeline
import java.net.URI
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class IntegrateNugetPackageJobTest {
  final pipeline = spy(DummyPipeline)
  final job = spy(new IntegrateNugetPackageJob(pipeline))

  @Before void beforeEachTest() {
    reset(job)
    job.gitSourceRef = new GitSourceReference('repository')
    doReturn([:]).when(job).checkoutSource(argThat(any(Map)))
    doNothing().when(job).markStageSkipped(argThat(instanceOf(String)))
  }

  @Test void run_does_checkout_in_checkout_stage() {
    // Act
    job.run()
    // Assert
    verify(pipeline).stage(argThat(equalTo('checkout')), argThat(any(Closure)))
    verify(job).checkout()
  }

  @Test void run_does_study_in_study_stage() {
    // Arrange
    doReturn(dir: 'source').when(job).checkout()
    // Act
    job.run()
    // Assert
    verify(pipeline).stage(argThat(equalTo('study')), argThat(any(Closure)))
    verify(job).study()
  }

  @Test void run_does_study_after_checkout() {
    // Arrange
    doReturn(dir: 'source').when(job).checkout()
    // Act
    job.run()
    // Assert
    final order = inOrder(job)
    order.verify(job).checkout()
    order.verify(job).study()
  }

  @Test void run_does_build_in_build_stage() {
    // Arrange
    doReturn(dir: 'source').when(job).checkout()
    doReturn(dockerfile: 'dockerfile').when(job).study()
    // Act
    job.run()
    // Assert
    verify(pipeline).stage(argThat(equalTo('build')), argThat(any(Closure)))
    verify(job).build()
  }

  @Test void run_does_build_after_equip() {
    // Arrange
    doReturn(dir: 'source').when(job).checkout()
    doReturn(dockerfile: 'dockerfile').when(job).study()
    // Act
    job.run()
    // Assert
    final order = inOrder(job)
    order.verify(job).equip()
    order.verify(job).build()
  }

  @Test void run_does_test_in_test_stage() {
    // Arrange
    job.image = 'image'
    // Act
    job.run()
    // Assert
    verify(pipeline).stage(argThat(equalTo('test')), argThat(any(Closure)))
    verify(job).test()
  }

  @Test void run_does_test_after_build() {
    // Arrange
    doReturn(dir: 'source').when(job).checkout()
    doReturn(dockerfile: 'dockerfile').when(job).study()
    doReturn('image').when(job).build()
    // Act
    job.run()
    // Assert
    final order = inOrder(job)
    order.verify(job).build()
    order.verify(job).test()
  }

  @Test void run_does_pack_in_pack_stage() {
    // Arrange
    job.image = 'image'
    // Act
    job.run()
    // Assert
    verify(pipeline).stage(argThat(equalTo('pack')), argThat(any(Closure)))
    verify(job).pack()
  }

  @Test void run_does_pack_after_test() {
    // Arrange
    job.image = 'image'
    // Act
    job.run()
    // Assert
    final order = inOrder(job)
    order.verify(job).test()
    order.verify(job).pack()
  }

  @Test void run_does_deliver_in_deliver_stage() {
    // Arrange
    job.packet = [[project: 'project']]
    doNothing().when(job).deliverPackages(any())
    // Act
    job.run()
    // Assert
    verify(pipeline).stage(argThat(equalTo('deliver')), argThat(any(Closure)))
    verify(job).deliver()
  }

  @Test void run_does_deliver_after_pack() {
    // Arrange
    job.image = 'image'
    doReturn([
      [project: 'project'],
    ]).when(job).pack()
    doNothing().when(job).deliverPackages(any())
    // Act
    job.run()
    // Assert
    final order = inOrder(job)
    order.verify(job).pack()
    order.verify(job).deliver()
  }

  @Test void checkout_checks_out_source_specified_by_git_source_reference() {
    // Arrange
    job.gitSourceRef = new GitSourceReference(
      host: 'host',
      owner: 'owner',
      repository: 'repository',
      branch: 'branch',
    )
    // Act
    job.checkout()
    // Assert
    verify(job).checkoutSource(argThat(
      allOf(
        hasEntry('baseUrl', 'host/owner'),
        hasEntry('repository', 'repository'),
        hasEntry('branch', 'branch'),
      )
    ))
  }

  @Test void has_default_git_config_when_created() {
    // Arrange
    final git = [:]
    git.with(DefaultValues.git)
    // Act
    final job = new IntegrateNugetPackageJob(pipeline)
    // Assert
    assertThat(job.gitConfig, allOf(
      hasProperty('baseUrl', equalTo(git.baseUrl)),
      hasProperty('credsId', equalTo(git.credsId)),
      hasProperty('branch', equalTo(git.branch)),
    ))
  }

  @Test void initialize_job_resolves_git_source_reference_against_git_config() {
    [
      'Take owner from config when reference has no owner',
      new GitConfig(owner: 'owner'),
      new GitSourceReference(repository: 'repository'),
      new GitSourceReference(owner: 'owner', repository: 'repository'),

      'Do not take owner from config when reference has owner',
      new GitConfig(owner: 'config'),
      new GitSourceReference(owner: 'reference'),
      new GitSourceReference(owner: 'reference'),

      'Take host from config when reference has no host',
      new GitConfig(host: 'host'),
      new GitSourceReference(owner: 'owner'),
      new GitSourceReference(host: 'host', owner: 'owner'),

      'Do not take host from config when reference has host',
      new GitConfig(host: 'config'),
      new GitSourceReference(host: 'reference', owner: 'owner'),
      new GitSourceReference(host: 'reference', owner: 'owner'),

      'Take scheme and host from config when reference has no host',
      new GitConfig(scheme: 'scheme', host: 'host'),
      new GitSourceReference(owner: 'owner'),
      new GitSourceReference(scheme: 'scheme', host: 'host', owner: 'owner'),

      'Do not take scheme from config when reference has scheme and has host',
      new GitConfig(scheme: 'config'),
      new GitSourceReference(scheme: 'reference', host: 'host'),
      new GitSourceReference(scheme: 'reference', host: 'host'),

      'Take scheme and host from config when reference has scheme but has no host',
      new GitConfig(scheme: 'config', host: 'host'),
      new GitSourceReference(scheme: 'reference'),
      new GitSourceReference(scheme: 'config', host: 'host'),

      'Do not take scheme from config when config and reference have no hosts',
      new GitConfig(scheme: 'config'),
      new GitSourceReference(scheme: 'reference'),
      new GitSourceReference(scheme: 'reference'),

      'Take branch from config when reference has no branch',
      new GitConfig(branch: 'branch'),
      new GitSourceReference(),
      new GitSourceReference(branch: 'branch'),

      'Do not take branch from config when reference has branch',
      new GitConfig(branch: 'config'),
      new GitSourceReference(branch: 'reference'),
      new GitSourceReference(branch: 'reference'),
    ]
    .collate(4)
    .each { test, config, reference, expected ->
      // Arrange
      job.gitConfig = config
      job.gitSourceRef = reference
      // Act
      job.initializeJob()
      // Assert
      assertThat("\n     For: ${test}",
        job.gitSourceRef, is(equalTo(expected)))
    }
  }

  @Test void checkout_uses_creds_specified_in_git_config() {
    // Arrange
    job.gitConfig = new GitConfig(credsId: 'creds')
    // Act
    job.checkout()
    // Assert
    verify(job).checkoutSource(argThat(
      hasEntry('credsId', 'creds'),
    ))
  }

  @Test void checkout_uses_job_pipeline() {
    // Act
    job.checkout()
    // Assert
    verify(job).checkoutSource(argThat(
      hasEntry(equalTo('pipeline'), is(sameInstance(pipeline))),
    ))
  }

  @Test void run_does_not_deliver_and_marks_deliver_stage_skipped_when_packet_is_null() {
    [
      null, [:],
    ].each { packet ->
      beforeEachTest()
      // Arrange
      job.packet = packet
      // Act
      job.run()
      // Assert
      verify(job, never()).deliver()
      verify(job).markStageSkipped('deliver')
    }
  }

  @Test void run_does_not_pack_and_marks_pack_stage_skipped_when_no_image() {
    [
      null, '',
    ].each { image ->
      beforeEachTest()
      // Arrange
      job.image = image
      // Act
      job.run()
      // Assert
      verify(job, never()).pack()
      verify(job).markStageSkipped('pack')
    }
  }

  @Test void run_does_not_test_and_marks_test_stage_skipped_when_no_image() {
    [
      null, '',
    ].each { image ->
      beforeEachTest()
      // Arrange
      job.image = image
      // Act
      job.run()
      // Assert
      verify(job, never()).test()
      verify(job).markStageSkipped('test')
    }
  }

  @Test void run_does_not_build_and_marks_build_stage_skipped_when_no_dockerfile() {
    [
      null, '',
    ].each { dockerfile ->
      beforeEachTest()
      // Arrange
      doReturn(dockerfile: dockerfile).when(job).checkout()
      // Act
      job.run()
      // Assert
      verify(job, never()).build()
      verify(job).markStageSkipped('build')
    }
  }

  @Test void run_does_not_study_and_marks_sudy_stage_skipped_when_no_source() {\
    [
      null, [:],
    ].each { source ->
      beforeEachTest()
      // Arrange
      job.source = source
      // Act
      job.run()
      // Assert
      verify(job, never()).study()
      verify(job).markStageSkipped('study')
    }
  }

  @Test void checkout_returns_source_dir_combined_from_checkedout_source_and_source_reference() {
    // Arrange
    doReturn(dir: 'root').when(job).checkoutSource(argThat(instanceOf(Map)))
    job.gitSourceRef = new GitSourceReference(directory: 'dir')
    // Act & Assert
    assertThat(job.checkout(), hasEntry('dir', 'root/dir'))
  }

  @Test void study_returns_source_dockerfile_if_it_exists_in_source_dir() {
    // Arrange
    job.source = [dir: 'source dir']
    doReturn(true).when(pipeline).fileExists('source dir/Dockerfile')
    // Act & Assert
    assertThat(job.study(), hasEntry('dockerfile', 'source dir/Dockerfile'))
  }

  @Test void study_returns_no_dockerfile_if_it_does_not_exist_in_source_dir() {
    // Arrange
    job.source = [dir: 'source dir']
    doReturn(false).when(pipeline).fileExists('source dir/Dockerfile')
    // Act & Assert
    assertThat(job.study(), not(hasEntry('dockerfile', 'source dir/Dockerfile')))
  }

  @Test void load_parameters_set_git_source_reference_branch_to_sha1_when_it_is_defined() {
    // Arrange
    job.gitSourceRef = new GitSourceReference(branch: 'branch')
    // Act
    job.loadParameters([sha1: 'sha1'])
    // Assert
    assertThat(job.gitSourceRef.branch, is(equalTo('sha1')))
  }

  @Test void load_parameters_leaves_git_source_reference_branch_intact_when_no_sha1_defined() {
    [
      null,
      [:],
      [sha1: null],
      [sha1: ''],
      [sha1: ' '],
    ].each { params ->
      // Arrange
      job.gitSourceRef = new GitSourceReference(branch: 'intact')
      // Act
      job.loadParameters(params)
      // Assert
      assertThat("\n     For: ${ params }",
        job.gitSourceRef.branch, is(equalTo('intact')))
    }
  }

  @Test void has_initialized_docker_when_created() {
    final job = new IntegrateNugetPackageJob(pipeline)
    assertThat(job.docker, is(not(null)))
  }

  @Test void builds_from_source_directory() {
    // Arrange
    job.source = [dir: 'source directory']
    job.docker = mock(DockerTool)
    // Act
    job.build()
    // Assert
    verify(job.docker).build(argThat(
      hasEntry('path', 'source directory')
    ))
  }

  @Test void has_default_nuget_config_when_created() {
    // Arrange
    final nuget = [:]
    nuget.with(DefaultValues.nuget)
    // Act
    final job = new IntegrateNugetPackageJob(pipeline)
    // Assert
    assertThat(job.nugetConfig, allOf(
      hasProperty('configRef', equalTo(nuget.configRef)),
    ))
  }

  @Test void defines_env_vars_and_volumes_for_nuget_config() {
    job.nugetConfig = mock(NugetConfig)
    when(job.nugetConfig.defineEnvVars(pipeline)).thenReturn(['nuget config', 'env'])
    assertThat(job.defineEnvVars(), contains('nuget config', 'env'))
  }

  @Test void defines_volumes_for_nuget_config() {
    job.nugetConfig = mock(NugetConfig)
    when(job.nugetConfig.defineVolumes(pipeline)).thenReturn(['nuget config', 'volume'])
    assertThat(job.defineVolumes(), contains('nuget config', 'volume'))
  }

  @Test void run_equips_checkedout_source_in_equip_stage() {
    // Arrange
    doReturn(dir: 'source').when(job).checkout()
    // Act
    job.run()
    // Assert
    final order = inOrder(pipeline, job)
    order.verify(pipeline).stage(argThat(equalTo('equip')), argThat(instanceOf(Closure)))
    order.verify(job).equip()
  }

  @Test void run_does_not_equip_when_no_source() {
    [
      null, [:]
    ].each { source ->
      beforeEachTest()
      // Arrange
      doReturn(source).when(job).checkout()
      // Act
      job.run()
      // Assert
      verify(job, never()).equip()
    }
  }

  @Test void run_equips_source_after_study() {
    // Arrange
    doReturn(dir: 'source').when(job).checkout()
    // Act
    job.run()
    // Assert
    final order = inOrder(job)
    order.verify(job).study()
    order.verify(job).equip()
  }

  @Test void equip_copies_nuget_config_file_into_source_dir() {
    // Arrange
    job.nugetConfig = spy(job.nugetConfig)
    when(job.nugetConfig.configPath).thenReturn('nuget config')
    job.source = [dir: 'source dir']
    // Act
    job.equip()
    // Assert
    verify(pipeline).sh(argThat(
      hasEntry(
        equalTo('script'),
        containsString('cp \'nuget config\' \'source dir/.nuget.config\'')
      )
    ))
  }

  @Test void equip_returns_equiped_source() {
    // Arrange
    job.nugetConfig = spy(job.nugetConfig)
    when(job.nugetConfig.configPath).thenReturn('nuget config')
    job.source = [dir: 'source dir']
    // Act
    final equiped = job.equip()
    // Assert
    assertThat(equiped, hasEntry('nugetConfig', '.nuget.config'))
  }

  @Test void build_passes_nuget_config_as_build_arg_to_docker() {
    // Arrange
    job.docker = mock(DockerTool)
    job.source = [nugetConfig: 'nuget config']
    // Act
    job.build()
    // Assert
    verify(job.docker).build(
      argThat(
        hasEntry(
          equalTo('buildArgs'),
          equalTo(nuget_conf_file: 'nuget config')
        )
      )
    )
  }

  @Test void run_collects_source_from_checkout_study_and_equip_steps() {
    // Arrange
    doReturn(checked: true).when(job).checkout()
    doReturn(studied: true).when(job).study()
    doReturn(equiped: true).when(job).equip()
    // Act
    job.run()
    // Assert
    assertThat(job.source, allOf(
      hasEntry('checked', true),
      hasEntry('studied', true),
      hasEntry('equiped', true),
    ))
  }

  @Test void has_default_nexus_config_when_created() {
    // Arrange
    final nexus = [:]
    nexus.with(DefaultValues.nexus)
    // Act
    final job = new IntegrateNugetPackageJob(pipeline)
    // Assert
    assertThat(job.nexusConfig, allOf(
      hasProperty('baseUrl', equalTo(nexus.baseUrl)),
      hasProperty('credsId', equalTo(nexus.credsId)),
    ))
  }

  @Test void build_logs_into_specified_docker_registry() {
    // Arrange
    job.docker = mock(DockerTool)
    job.source = [:]
    job.dockerRegistry = new URI('registry')
    job.dockerRegistry.userInfo = 'user:password'
    // Act
    job.build()
    // Assert
    verify(job.docker).login(
      argThat(allOf(
        hasEntry('server', 'registry'),
        hasEntry('username', 'user'),
        hasEntry('password', 'password'),
      ))
    )
  }

  @Test void initialize_job_set_docker_registry_from_nexus_config_when_the_registry_is_not_defined() {
    // Arrange
    job.dockerRegistry = null
    job.nexusConfig.baseUrl = 'nexus-url'
    job.nexusConfig.port = 10
    job.nexusConfig.credsId = 'nexus-creds'
    doReturn(['nexus-user', 'nexus-password']).when(job).getUsernamePassword('nexus-creds')
    // Act
    job.initializeJob()
    // Assert
    assertThat(job.dockerRegistry,
      is(allOf(
        equalTo(new URI('nexus-url:10')),
        hasProperty('userInfo', equalTo('nexus-user:nexus-password')),
      ))
    )
  }

  @Test void initialize_job_leaves_docker_registry_intact_if_is_defined() {
    // Arrange
    job.dockerRegistry = new URI('intact')
    // Act
    job.initializeJob()
    // Assert
    assertThat(job.dockerRegistry, hasToString('intact'))
  }

  @Test void build_uses_host_network() {
    // Arrange
    job.source = [dir: 'source']
    job.docker = mock(DockerTool)
    // Act
    job.build()
    // Assert
    verify(job.docker).build(
      argThat(
        hasEntry('network', 'host')
      )
    )
  }

  @Test void study_searches_csproj_files_within_source_directory() {
    // Arrange
    job.source = [dir: 'source']
    job.fileFinder = mock(FindTool)
    when(job.fileFinder.find(
      argThat(
        allOf(
          hasEntry('directory', 'source'),
          hasEntry('name', '*.csproj'),
        )
      )
    )).thenReturn(['first', 'second'])
    // Act & Assert
    assertThat(job.study(),
      hasEntry(
        equalTo('projects'),
        contains('first', 'second')
      )
    )
  }

  @Test void study_returns_empty_list_of_projects_when_no_csproj_file_found() {
    // Arrange
    job.source = [dir: 'source']
    job.fileFinder = mock(FindTool)
    when(job.fileFinder.find(
      argThat(
        allOf(
          hasEntry('directory', 'source'),
          hasEntry('name', '*.csproj'),
        )
      )
    )).thenReturn([])
    // Act & Assert
    assertThat(job.study(),
      hasEntry(
        equalTo('projects'),
        equalTo([])
      )
    )
  }

  @Test void pack_returns_only_items_that_have_nugets() {
    // Arrange
    final projects = [
      'empty nugets',
      'null nugets',
      'has nugets',
    ]
    job.source = [projects: projects]
    doReturn([
      [project: 'empty nugets', nugets: []],
      [project: 'null nugets', nugets: null],
      [project: 'has nugets', nugets: ['package']],
    ]).when(job).packCsProjects(projects)
    // Act & Assert
    assertThat(job.pack(), contains([project: 'has nugets', nugets: ['package']]))
  }

  @Test void deliver_packages_checks_existence_of_each_package() {
    // Arrange
    final packages = [
      [name: 'first', version: '1', nugets: ['first']],
      [name: 'second', version: '2', nugets: ['second']],
    ]
    final repository = mock(NugetRepository)
    doReturn(repository).when(job).createNugetRepository()
    // Act
    job.deliverPackages(packages)
    // Assert
    final order = inOrder(repository)
    order.verify(repository).hasNuget(name: 'first', version: '1')
    order.verify(repository).hasNuget(name: 'second', version: '2')
  }

  @Test void publishes_prerelease_version_by_default() {
    final job = new IntegrateNugetPackageJob(pipeline)
    assertThat(job.publishPrereleaseVersion, is(true))
  }

  @Test void does_not_prerelases_into_packet_when_asked_to_skip_them() {
    // Arrange
    job.publishPrereleaseVersion = false
    final projects = [
      'first',
      'second',
    ]
    final packages = [
      [nugets: ['first'], version: '1.2.3'],
      [nugets: ['first'], version: '1.2.3-a'],
      [nugets: ['second'], version: '1.0.0'],
      [nugets: ['second'], version: '1.0.0.1'],
    ]
    job.source = [
      projects: projects
    ]
    doReturn(packages).when(job).packCsProjects(projects)
    // Act
    final packet = job.pack()
    // Assert
    assertThat(packet, is(equalTo([
      [nugets: ['first'], version: '1.2.3'],
      [nugets: ['second'], version: '1.0.0'],
    ])))
  }
  
  @Test void test_publishes_test_results_from_all_trx_files_under_test_result_directory() {
    // Arrange
    job.testResultDir = 'test results'
    // Act
    job.test()
    // Assert
    verify(pipeline).step(argThat(
      hasEntry('testResultsFile', 'test results/*.trx')
    ))
  }

  @Test void run_does_not_do_checkout_when_git_source_reference_is_invalid() {
    // Arrange
    job.gitSourceRef = mock(GitSourceReference)
    when(job.gitSourceRef.isValid).thenReturn(false)
    // Act
    job.run()
    // Assert
    verify(job, never()).checkout()
    verify(job).markStageSkipped('checkout')
  }
}
