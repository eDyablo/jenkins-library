package com.e4d.dotnet

import com.e4d.dotnet.DotnetTool
import com.e4d.shell.Shell

import org.junit.*
import org.mockito.ArgumentCaptor
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any

class DotnetToolTest extends DotnetTool {
  final def shellArgs = ArgumentCaptor.forClass(Map.class)

  DotnetToolTest() {
    super(mock(Object.class), mock(Shell.class))
  }

  def getShellScript() {
    shellArgs.value.script
  }

  def getShellCommands() {
    shellArgs.value.script.split('\n') as List<String>
  }

  @Test void nuget_push_searches_for_nupkg_with_specified_name() {
    [
      'package', 'package.nupkg',
      'package.nupkg', 'package.nupkg',
    ]
    .collate(2)
    .each { pkg, pattern ->
      // Arrange
      reset(shell)
      // Act
      nugetPush(pkg)
      // Assert
      verify(shell).execute(shellArgs.capture(), any(List))
      assertThat(shellScript, stringContainsInOrder(
        'find ', "-name \'${ pattern }\'"
      ))
    }
  }

  @Test void nuget_push_uses_specified_source() {
    nugetPush('package', source: 'source')
    verify(shell).execute(shellArgs.capture(), any(List))
    assertThat(shellScript, stringContainsInOrder(
      'nuget push', '--source', 'source'
    ))
  }

  @Test void nuget_push_uses_specified_api_key() {
    nugetPush('package', api_key: 'key')
    verify(shell).execute(shellArgs.capture(), any(List))
    assertThat(shellScript, stringContainsInOrder(
      'nuget push', '--api-key', 'key'
    ))
  }

  @Test void nuget_push_does_not_search_for_nupkg_when_specified_package_path() {
    nugetPush('/package')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          allOf(
            not(containsString('find -name')),
            containsString('package=\'/package\''),
          )
        )
      ),
      argThat(equalTo([]))
    )
  }

  @Test void csproj_pack_packs_csproj_file_found_for_specified_name() {
    [
      'project', 'project.csproj',
      'project.csproj', 'project.csproj',
    ]
    .collate(2)
    .each { project, pattern ->
      reset(shell)
      csprojPack(project)
      verify(shell).execute(
        argThat(
          hasEntry(
            equalTo('script'),
            stringContainsInOrder(
              'project=', "find -name '${ pattern }'",
              'project_output=',
              'dotnet pack', '--output="${project_output}"', '--no-build', '"${project}"'
            )
          )
        ),
        argThat(
          equalTo([])
        )
      )
    }
  }

  @Test void csproj_pack_uses_specified_version() {
    csprojPack('project', version: 'version')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'dotnet pack', '-p:Version=version', '"${project}"'
          )
        )
      ),
      argThat(
        equalTo([])
      )
    )
  }

  @Test void csproj_pack_uses_specified_version_prefix() {
    csprojPack('project', versionPrefix: 'prefix')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'dotnet pack', '-p:VersionPrefix=prefix', '"${project}"'
          )
        )
      ),
      argThat(
        equalTo([])
      )
    )
  }

  @Test void csproj_pack_uses_specified_version_suffix() {
    csprojPack('project', versionSuffix: 'suffix')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          stringContainsInOrder(
            'dotnet pack', '--version-suffix=suffix', '"${project}"'
          )
        )
      ),
      argThat(
        equalTo([])
      )
    )
  }

  @Test void csproj_pack_does_not_specify_version_version_prefix_and_version_suffix_when_they_are_no_specified() {
    csprojPack('project')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          allOf(
            not(containsString('-p:Version=')),
            not(containsString('-p:VersionPrefix=')),
            not(containsString('--version-suffix=')),
          )
        )
      ),
      argThat(
        equalTo([])
      )
    )
  }

  @Test void csproj_pack_echos_result_as_json() {
    csprojPack('project')
    verify(shell).execute(
      argThat(
        hasEntry(
          equalTo('script'),
          containsString('''
            echo "{
              \\"selector\\": \\"project.csproj\\",
              \\"project\\": \\"${project}\\",
              \\"package\\": \\"${package}\\"
            }"
          '''.stripIndent().trim())
        )
      ),
      argThat(
        equalTo([])
      )
    )
  }

  @Test void csproj_pack_returns_result_retrieved_from_shell_output() {
    final output = '''
    output
    {
      "project": "project",
      "package": "package"
    }
    output
    '''.stripIndent().trim()
    doReturn(output).when(shell).execute(
      argThat(hasEntry('returnStdout', true)),
      argThat(equalTo([]))
    )
    assertThat(csprojPack('project'), allOf(
      hasEntry('project', 'project'),
      hasEntry('package', 'package'),
    ))
  }

  @Test void csproj_pack_returns_version_extracted_from_resulting_nuget_package() {
    [
      '/input/project.csproj',
      '/output/project.1.2.3.nupkg',
      '1.2.3',

      'project',
      'project.1.2.3-a.4.nupkg',
      '1.2.3-a.4',

      'project',
      'project.1.2.3-a.4',
      '1.2.3-a.4',

      '',
      '',
      '',
    ]
    .collate(3)
    .each { project, pkg, version ->
      doReturn("""{
        "project": "${ project }",
        "package": "${ pkg }"
      }""").when(shell).execute(
        argThat(hasEntry('returnStdout', true)),
        argThat(equalTo([]))
      )
      assertThat(csprojPack('project'), hasEntry(
        'version', version
      ))
      reset(shell)
    }
  }
}
