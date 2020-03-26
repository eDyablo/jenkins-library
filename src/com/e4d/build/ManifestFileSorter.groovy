package com.e4d.build

/**
 * Module that prepares sequence of manifest file sets.
 */
class ManifestFileSorter {
  private def lookup
  private def prefixes

  /**
   * Triages paths.
   *
   * @param options A map of arbitrary options.
   *        prefixes A list of strings that will be removed from the paths.
   * @return list of {@code @ManifestFileSet}
   */
  def triage(final Map options=[:], final List<String> paths) {
    prefixes = sanitizePrefixes(options.prefixes ?: [ options.prefix ])
    lookup = [:]
    paths.each { add(it) }
    lookup.values()
  }

  /**
   * Triages paths. See {@code @triage}
   */
  def triage(final Map options=[:], final String[] paths) {
    triage(options, paths as List<String>)
  }

  private def sanitizePrefixes(final List<String> prefixes) {
    prefixes
      .findAll { it }
      .sort { first, second -> second.size() <=> first.size() }
  }

  private void add(final String path) {
    final def set = findSet(path)
    set.paths.add(path)
  }

  private def findSet(final String path) {
    final def name = extractNamespace(path)
    def set = lookup.get(name)
    if (!set) {
      set = new ManifestFileSet(paths: [], namespace: name)
      lookup.put(name, set)
    }
    return set
  }

  private String extractNamespace(final String path) {
    removePrefix(path).tokenize('/').init().join('-')
  }

  private String removePrefix(final String path) {
    final def prefix = prefixes.find {
      path.startsWith(it)
    }
    path[(prefix?.size() ?: 0)..-1]
  }
}
