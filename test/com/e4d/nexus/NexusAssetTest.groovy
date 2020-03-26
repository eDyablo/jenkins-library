package com.e4d.nexus

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class NexusAssetTest {
  @Test void newly_created_has_defaults() {
    final asset = new NexusAsset()
    assertThat(asset, allOf(
      hasProperty('id', is(null)),
      hasProperty('downloadUrl', is(null)),
      hasProperty('path', is(null)),
      hasProperty('checksum', is([sha1: null, sha512: null])),
      hasProperty('name', is(null)),
      hasProperty('version', is(null)),
    ))
  }

  @Test void gets_name_and_version_from_its_path() {
    final tests = [
      [path: null, name: null, version: null],
      [path: '', name: null, version: null],
      [path: ' \t  ', name: '', version: null],
      [path: 'path', name: 'path', version: null],
      [path: 'path/version', name: 'path', version: 'version'],
      [path: '/version', name: null, version: 'version'],
      [path: '/', name: null, version: null],
      [path: ' \t  / \t  ', name: '', version: ''],
    ]
    tests.each { test ->
      final asset = new NexusAsset(path: test.path)
      assertThat("\n     For: ${ test.path }",
        asset, allOf(
          hasProperty('name', is(equalTo(test.name))),
          hasProperty('version', is(equalTo(test.version))),
      ))
    }
  }
}
