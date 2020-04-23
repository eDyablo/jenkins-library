package com.e4d.git

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class GitSourceReferenceTest {
  @Test void has_default_values_when_created() {
    final reference = new GitSourceReference()
    assertThat(reference, allOf(
      hasProperty('branch', equalTo(null)),
      hasProperty('directory', equalTo(null)),
      hasProperty('host', equalTo(null)),
      hasProperty('owner', equalTo(null)),
      hasProperty('repository', equalTo(null)),
      hasProperty('scheme', equalTo(null)),
    ))
  }

  @Test void has_proper_values_when_created_from_text() {
    [
      ['repository', [
        scheme: null,
        host: null,
        owner: null,
        repository: 'repository',
        branch: null,
        directory: null,
      ]],
      ['owner/repository', [
        scheme: null,
        host: null,
        owner: 'owner',
        repository: 'repository',
        branch: null,
        directory: null,
      ]],
      ['host/owner/repository', [
        scheme: null,
        host: 'host',
        owner: 'owner',
        repository: 'repository',
        branch: null,
        directory: null,
      ]],
      ['scheme://host/owner/repository', [
        scheme: 'scheme',
        host: 'host',
        owner: 'owner',
        repository: 'repository',
        branch: null,
        directory: null,
      ]],
      ['repository/tree/branch', [
        scheme: null,
        host: null,
        owner: null,
        repository: 'repository',
        branch: 'branch',
        directory: null,
      ]],
      ['owner/repository/tree/branch', [
        scheme: null,
        host: null,
        owner: 'owner',
        repository: 'repository',
        branch: 'branch',
        directory: null,
      ]],
      ['repository/tree/branch/first/second', [
        scheme: null,
        host: null,
        owner: null,
        repository: 'repository',
        branch: 'branch',
        directory: 'first/second',
      ]],
      ['owner/repository/tree/branch/first/second', [
        scheme: null,
        host: null,
        owner: 'owner',
        repository: 'repository',
        branch: 'branch',
        directory: 'first/second',
      ]],
      ['host/owner/repository/tree/*/first/second', [
        scheme: null,
        host: 'host',
        owner: 'owner',
        repository: 'repository',
        branch: null,
        directory: 'first/second',
      ]],
      ['repository/tree/*', [
        scheme: null,
        host: null,
        owner: null,
        repository: 'repository',
        branch: null,
        directory: null,
      ]],
      ['', [
        scheme: null,
        host: null,
        owner: null,
        repository: null,
        branch: null,
        directory: null,
      ]],
      [null, [
        scheme: null,
        host: null,
        owner: null,
        repository: null,
        branch: null,
        directory: null,
      ]],
    ].each { text, expected ->
      final reference = new GitSourceReference(text)
      expected.each { name, value ->
        assertThat("\n     For: '${ text }'",
          reference, hasProperty(name, equalTo(value)))
      }
    }
  }

  @Test void returns_properly_constructed_urls() {
    [
      [new GitSourceReference(host: 'host'),
        [
          url: null,
          hostUrl: 'host',
        ]
      ],
      [new GitSourceReference(host: 'host', owner: 'owner'),
        [
          url: null,
          hostUrl: 'host',
          organizationUrl: 'host/owner',
        ]
      ],
      [new GitSourceReference(host: 'host', owner: 'owner',
        repository: 'repository'),
        [
          url: null,
          hostUrl: 'host',
          organizationUrl: 'host/owner',
          repositoryUrl: 'host/owner/repository',
        ]
      ],
      [new GitSourceReference(host: 'host', owner: 'owner',
        repository: 'repository', branch: 'branch'), 
        [
          url: 'host/owner/repository/tree/branch',
          hostUrl: 'host',
          organizationUrl: 'host/owner',
          repositoryUrl: 'host/owner/repository',
        ]
      ],
      [new GitSourceReference(host: 'host', owner: 'owner',
        repository: 'repository', branch: 'branch', directory: 'first/second'),
        [
          url: 'host/owner/repository/tree/branch/first/second',
          hostUrl: 'host',
          organizationUrl: 'host/owner',
          repositoryUrl: 'host/owner/repository',
        ]
      ],
      [new GitSourceReference(host: 'host', owner: 'owner',
        repository: 'repository', directory: 'first/second'),
        [
          url: 'host/owner/repository/tree/*/first/second',
          hostUrl: 'host',
          organizationUrl: 'host/owner',
          repositoryUrl: 'host/owner/repository',
        ]
      ],
      [new GitSourceReference(scheme: 'scheme', host: 'host', owner: 'owner',
        repository: 'repository', directory: 'first/second'),
        [
          url: 'scheme://host/owner/repository/tree/*/first/second',
          hostUrl: 'scheme://host',
          organizationUrl: 'scheme://host/owner',
          repositoryUrl: 'scheme://host/owner/repository',
        ]
      ],
      [new GitSourceReference(directory: 'first/second'),
        [
          url: null,
          hostUrl: null,
          organizationUrl: null,
          repositoryUrl: null,
        ]
      ],
      [new GitSourceReference(branch: 'branch', directory: 'first/second'),
        [
          url: null,
          hostUrl: null,
          organizationUrl: null,
          repositoryUrl: null,
        ]
      ],
      [new GitSourceReference(owner: 'owner', branch: 'branch', directory: 'first/second'),
        [
          url: null,
          hostUrl: null,
          organizationUrl: null,
          repositoryUrl: null,
        ]
      ],
      [new GitSourceReference(host: 'host', owner: 'owner', branch: 'branch'),
        [
          url: null,
          hostUrl: 'host',
          organizationUrl: 'host/owner',
          repositoryUrl: null,
        ]
      ],
    ].each { reference, expected ->
      expected.each { name, value ->
        assertThat("\n     For: '${ reference }'",
          reference, hasProperty(name, equalTo(value)))
      }
    }
  }

  @Test void to_string_returns_proper_textual_representation() {
    [
      [new GitSourceReference(), ''],
      [new GitSourceReference(null), ''],
      [new GitSourceReference(''), ''],
      [new GitSourceReference(host: 'host'),
        'host/?/?'],
      [new GitSourceReference(scheme: 'scheme', host: 'host'),
        'scheme://host/?/?'],
      [new GitSourceReference(host: 'host', owner: 'owner'),
        'host/owner/?'],
      [new GitSourceReference(host: 'host', owner: 'owner',
        repository: 'repository'),
        'host/owner/repository'],
      [new GitSourceReference(host: 'host', owner: 'owner',
        repository: 'repository', branch: 'branch'),
        'host/owner/repository/tree/branch'],
      [new GitSourceReference(host: 'host', owner: 'owner',
        repository: 'repository', branch: 'branch', directory: 'first/second'),
        'host/owner/repository/tree/branch/first/second'],
      [new GitSourceReference(host: 'host', owner: 'owner',
        repository: 'repository', directory: 'first/second'),
        'host/owner/repository/tree/*/first/second'],
      [new GitSourceReference(repository: 'repository'),
        'repository'],
      [new GitSourceReference(owner: 'owner', repository: 'repository'),
        'owner/repository'],
      [new GitSourceReference(owner: 'owner', repository: 'repository',
        branch: 'branch'),
        'owner/repository/tree/branch'],
      [new GitSourceReference(owner: 'owner', repository: 'repository',
        branch: 'branch', directory: 'first/second'),
        'owner/repository/tree/branch/first/second'],
      [new GitSourceReference(host: 'host', repository: 'repository'),
        'host/?/repository'],
      [new GitSourceReference(owner: 'owner'),
        'owner/?'],
    ].each { reference, expected ->
      assertThat(reference, hasToString(equalTo(expected)))
    }
  }

  @Test void is_valid_when_has_enough_components_defined_to_refer_a_source() {
    [
      new GitSourceReference(repository: 'repository'),
      new GitSourceReference(owner: 'owner', repository: 'repository'),
      new GitSourceReference(owner: 'owner', repository: 'repository', branch: 'branch'),
      new GitSourceReference(owner: 'owner', repository: 'repository', directory: 'directory'),
      new GitSourceReference(repository: 'repository', directory: 'directory'),
      new GitSourceReference(host: 'host', owner: 'owner', repository: 'repository', directory: 'directory'),
    ].each { reference ->
      assertThat("\n     For: '${ reference }'",
        reference.isValid, is(equalTo(true)))
    }
  }

  @Test void is_invalid_when_has_not_enough_components_defined_to_refer_a_source() {
    [
      new GitSourceReference(),
      new GitSourceReference(null),
      new GitSourceReference(repository: null),
      new GitSourceReference(owner: 'owner'),
      new GitSourceReference(host: 'host'),
      new GitSourceReference(host: 'host', owner: 'owner'),
      new GitSourceReference(host: 'host', repository: 'repository'),
      new GitSourceReference(branch: 'branch'),
      new GitSourceReference(directory: 'directory'),
      new GitSourceReference(branch: 'branch', directory: 'directory'),
      new GitSourceReference(host: 'host', branch: 'branch', directory: 'directory'),
      new GitSourceReference(host: 'host', directory: 'directory'),
      new GitSourceReference(owner: 'owner', branch: 'branch', directory: 'directory'),
      new GitSourceReference(owner: 'owner', directory: 'directory'),
    ].each { reference ->
      assertThat("\n     For: '${ reference }'",
        reference.isValid, is(equalTo(false)))
    }
  }

  @Test void is_absolute_when_has_host() {
    final reference = new GitSourceReference(host: 'host')
    assertThat(reference.isAbsolute, is(equalTo(true)))
  }

  @Test void is_relative_when_has_no_host() {
    [
      new GitSourceReference(),
      new GitSourceReference(host: null),
      new GitSourceReference(host: ''),
      new GitSourceReference(owner: 'owner'),
      new GitSourceReference(host: null, owner: 'owner'),
      new GitSourceReference(host: '', owner: 'owner'),
    ].each { reference ->
      assertThat("\n     For: '${ reference }'",
        reference.isRelative, is(equalTo(true)))
    }
  }

  @Test void equals() {
    [
      new GitSourceReference(),
      new GitSourceReference(),
      true,
      new GitSourceReference(scheme: 'http'),
      new GitSourceReference(scheme: 'http'),
      true,
      new GitSourceReference(scheme: 'http'),
      new GitSourceReference(scheme: 'https'),
      false,
      new GitSourceReference(host: 'host'),
      new GitSourceReference(host: 'host'),
      true,
      new GitSourceReference(host: 'host'),
      new GitSourceReference(host: 'host1'),
      false,
      new GitSourceReference(owner: 'owner'),
      new GitSourceReference(owner: 'owner'),
      true,
      new GitSourceReference(owner: 'owner'),
      new GitSourceReference(owner: 'owner1'),
      false,
      new GitSourceReference(repository: 'repository'),
      new GitSourceReference(repository: 'repository'),
      true,
      new GitSourceReference(repository: 'repository'),
      new GitSourceReference(repository: 'repository1'),
      false,
      new GitSourceReference(branch: 'branch'),
      new GitSourceReference(branch: 'branch'),
      true,
      new GitSourceReference(branch: 'branch'),
      new GitSourceReference(branch: 'branch1'),
      false,
      new GitSourceReference(directory: 'dir'),
      new GitSourceReference(directory: 'dir'),
      true,
      new GitSourceReference(directory: 'dir/'),
      new GitSourceReference(directory: 'dir/1'),
      false,
      new GitSourceReference(scheme: 's', host: 'h'),
      new GitSourceReference(scheme: 's', host: 'h'),
      true,
      new GitSourceReference(scheme: 's', host: 'h', owner: 'o'),
      new GitSourceReference(scheme: 's', host: 'h', owner: 'o'),
      true,
      new GitSourceReference(scheme: 's', host: 'h', owner: 'o', repository: 'r'),
      new GitSourceReference(scheme: 's', host: 'h', owner: 'o', repository: 'r'),
      true,
      new GitSourceReference(scheme: 's', host: 'h', owner: 'o', repository: 'r', branch: 'b'),
      new GitSourceReference(scheme: 's', host: 'h', owner: 'o', repository: 'r', branch: 'b'),
      true,
      new GitSourceReference(scheme: 's', host: 'h', owner: 'o', repository: 'r', branch: 'b', directory: 'd'),
      new GitSourceReference(scheme: 's', host: 'h', owner: 'o', repository: 'r', branch: 'b', directory: 'd'),
      true,
    ].collate(3).each { first, second, expected ->
      assertThat("\n     For: '${ first }' == '${ second }'",
        first == second, is(expected))
    }
  }

  @Test void with_branch_returns_new_reference_copied_from_origin_and_with_new_branch() {
    final origin = new GitSourceReference(
      branch: 'origin branch',
      directory: 'origin directory',
      host: 'origin host',
      owner: 'origin owner',
      repository: 'origin repository',
      scheme: 'origin scheme',
    )
    assertThat(origin.withBranch('new branch'), allOf(
      hasProperty('branch', equalTo('new branch')),
      hasProperty('directory', equalTo('origin directory')),
      hasProperty('host', equalTo('origin host')),
      hasProperty('owner', equalTo('origin owner')),
      hasProperty('repository', equalTo('origin repository')),
      hasProperty('scheme', equalTo('origin scheme')),
    ))
  }
}
