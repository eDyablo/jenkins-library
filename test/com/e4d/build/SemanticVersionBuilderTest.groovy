package com.e4d.build

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class SemanticVersionBuilderTest {
  @Test void builds_zero_version_by_default() {
    def version = new SemanticVersionBuilder().build()
    assertThat(version, equalTo(SemanticVersion.ZERO))
  }

  @Test void builds_version_according_to_instuctions() {
    final int[] emptyInts = []
    final def testData = [
      [         '1.0.0', { major(1) } ],
      [         '0.1.0', { minor(1) } ],
      [         '0.0.1', { patch(1) } ],
      [         '1.2.3', { major(1).minor(2).patch(3) } ],
      [         '3.2.1', { patch(1).minor(2).major(3) } ],
      [       '0.0.0+1', { build(1) } ],
      [       '0.0.0+a', { build('a') } ],
      [     '0.0.0+a.1', { build('a').build(1) } ],
      [       '0.0.0-1', { prerelease(1) } ],
      [       '0.0.0-a', { prerelease('a') } ],
      [     '0.0.0-a.1', { prerelease('a').prerelease(1) } ],
      [ '1.2.3-a.4+b.5', { major(1).minor(2).patch(3).prerelease('a').prerelease(4).build('b').build(5) } ],
      [         '0.0.0', { build(1).resetBuild() }],
      [         '0.0.0', { prerelease(1).resetPrerelease() }],
      [         '0.0.0', { build(1).prerelease(1).resetPrerelease().resetBuild() }],
      [         '0.0.0', { prerelease(null) }],
      [         '0.0.0', { prerelease(' \t  ') }],
      [         '0.0.0', { build(null) }],
      [         '0.0.0', { build(' \t  ') }],
      [         '1.2.3', { core('1.2.3') }],
      [     '1.2.3+4.5', { core('1.2.3.4.5') }],
      [         '0.0.0', { core(null as String) }],
      [         '0.0.0', { core(' \t  ') }],
      [         '1.2.3', { core(1, 2, 3) }],
      [         '1.0.0', { core(1) }],
      [         '1.2.0', { core(1, 2) }],
      [         '0.0.0', { core(emptyInts) }],
      [     '1.2.3+4.5', { core(1, 2, 3, 4, 5) }],
    ]
    testData.each { expected, instructions ->
      final def builder = new SemanticVersionBuilder()
      builder.with(instructions)
      assertThat(builder.build().toString(), equalTo(expected))
    }
  }

  @Test void builds_version_from_git_tag() {
    final def testData = [
      [                      null, '0.0.0' ],
      [                        '', '0.0.0' ],
      [                       '1', '1.0.0' ],
      [                     '1.0', '1.0.0' ],
      [                     '1.1', '1.1.0' ],
      [                   '1.1.1', '1.1.1' ],
      [                  'v1.2.3', '1.2.3' ],
      [              'v1.2.3.4.5', '1.2.3+4.5' ],
      [                 '1.2.3-4', '1.2.3-4' ],
      [               '1.2.3-4-5', '1.2.3-4.5' ],
      [        '1.2.3-4-gdeadbee', '1.2.3+git.hdeadbee' ],
      [                 'deadbee', '0.0.0+git.hdeadbee' ],
      [                 '1234567', '0.0.0+git.h1234567' ],
      [                '12345678', '0.0.0+git.h12345678' ],
      [                 '1.23456', '1.23456.0' ],
      [                 '12.3456', '12.3456.0' ],
      [                 '123.456', '123.456.0' ],
      [                 '12345.6', '12345.6.0' ],
      [ 'v1.2.3-alpha-4-gdeadbee', '1.2.3-alpha+git.hdeadbee' ],
      [            'v1.2.3-alpha', '1.2.3-alpha' ],
      [       '12345.12345.12345', '12345.12345.12345' ],
    ]
    testData.each { tag, expected ->
      final def version = new SemanticVersionBuilder().fromGitTag(tag).build()
      assertThat("\n     For: ${ tag }", version.toString(), equalTo(expected))
    }
  }

  @Test void from_git_tag_resets_build_and_prerelease() {
    final def version = new SemanticVersionBuilder()
      .prerelease('a').build(1).fromGitTag('1').build()
    assertThat(version, equalTo(new SemanticVersion(major: 1)))
  }
}
