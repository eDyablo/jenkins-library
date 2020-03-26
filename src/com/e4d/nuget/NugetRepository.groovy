package com.e4d.nuget

/**
 * Declares interface for repository stores nuget packages.
 */
interface NugetRepository {
  /**
   * Determines if the repository contains a nuget package.
   *
   * @param options A map of arbitrary options.
   *        options.name A string defines name a the package,
   *        options.version A string defines version a the package.
   */
  boolean hasNuget(Map options)
}
