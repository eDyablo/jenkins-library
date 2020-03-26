package com.e4d.nexus

import com.e4d.nuget.NugetRepository

/**
 * Defines nuget package repository handled by Nexus
 */
class NexusNugetRepository implements NugetRepository {
  final NexusClient client
  final String repository

  final static String REPOSITORY = 'debug-nugets'

  /**
   * Constructs the repository.
   *
   * @param options A map of arbitrary options.
   *        options.client A reference to an object implements NexusClient,
   *        options.repository A string defines name of a repository.
   */
  NexusNugetRepository(Map options) {
    client = options.client
    repository = options.repository ?: REPOSITORY
  }

  /**
   * Determines if the repository contains a nuget package.
   *
   * @param tag A string contains name and optionaly version of a package.
   *            The name and the version must be separated by column.
   */
  boolean hasNuget(String tag) {
    final def parts = (tag+':').split(':', -1)
    hasNuget(name: parts[0], version: parts[1])
  }

  /**
   * Determines if the repository contains a nuget package.
   * See {@code @NugetRepository}.
   */
  boolean hasNuget(Map options) {
    client.searchComponents(
      repository: repository,
      name: options.name,
      version: options.version,
    )?.size() > 0
  }
}
