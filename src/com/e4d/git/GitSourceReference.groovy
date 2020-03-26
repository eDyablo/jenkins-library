package com.e4d.git

import com.cloudbees.groovy.cps.NonCPS

class GitSourceReference {
  final String branch
  final String directory
  final String host
  final String owner
  final String repository
  final String scheme

  GitSourceReference(Map options=[:]) {
    if (options) {
      branch = options.branch
      directory = options.directory
      host = options.host
      owner = options.owner
      repository = options.repository
      scheme = options.scheme
    }
  }

  GitSourceReference(String text) {
    final parser = new Parser()
    parser.parse(text)
    branch = parser.branch
    directory = parser.directory
    host = parser.host
    owner = parser.owner
    repository = parser.repository
    scheme = parser.scheme
  }

  String getUrl() {
    if (repositoryUrl && path) {
      [repositoryUrl, path].join('/tree/')
    }
  }

  String getRepositoryUrl() {
    if (organizationUrl && repository) {
      [organizationUrl, repository].join('/')
    }
  }

  String getOrganizationUrl() {
    if (hostUrl) {
      [hostUrl, owner].findAll().join('/')
    }
  }

  String getHostUrl() {
    if (host) {
      [scheme, host].findAll().join('://')
    }
  }

  String getPath() {
    if (directory) {
      [branch ?: '*', directory].findAll().join('/')
    } else {
      branch
    }
  }

  String getOrganization() {
    owner
  }

  static class Parser {
    String branch
    String directory
    String host
    String owner
    String repository
    String scheme

    @NonCPS
    void parse(String text) {
      final parts = text.split('://', 2)
      if (parts.size() > 1) {
        scheme = parts[0]
        parseResourcePart(parts[1])
      } else {
        parseResourcePart(text)
      }
    }

    @NonCPS
    void parseResourcePart(String text) {
      final parts = text.split('/tree/', 2)
      if (parts.size() > 1) {
        parseRepositoryPart(parts[0])
        parseBranchPart(parts[1])
      } else {
        parseRepositoryPart(text)
      }
    }

    @NonCPS
    void parseRepositoryPart(String text) {
      if (!text) {
        return
      }
      final parts = text.split('/', 3)
      if (parts.size() > 2) {
        host = parts[0]
        owner = parts[1]
        repository = parts[2]
      } else if (parts.size() > 1) {
        owner = parts[0]
        repository = parts[1]
      } else {
        repository = parts[0]
      }
    }

    @NonCPS
    void parseBranchPart(String text) {
      final parts = text.split('/', 2)
      if (parts.size() > 1) {
        setBranch(parts[0])
        directory = parts[1]
      } else {
        setBranch(parts[0])
      }
    }

    @NonCPS
    void setBranch(String value) {
      branch = (value == '*') ? null : value
    }
  }

  String toString() {
    [
      [
        [
          scheme,
          host,
        ].findAll().join('://'),
        owner ?: (host ? '?' : null),
        repository ?: (host || owner ? '?' : null)
      ].findAll().join('/'),
      [
        branch ?: (directory ? '*' : null),
        directory
      ].findAll().join('/'),
    ].findAll().join('/tree/')
  }

  boolean getIsValid() {
    (owner && repository && !host) ||
    (repository && !owner && !host)
  }

  boolean getIsAbsolute() {
    host
  }

  boolean getIsRelative() {
    !isAbsolute
  }

  boolean equals(other) {
    if (other instanceof GitSourceReference) {
      directory == other.directory &&
      branch == other.branch &&
      repository == other.repository &&
      owner == other.owner &&
      host == other.host &&
      scheme == other.scheme
    }
  }
}
