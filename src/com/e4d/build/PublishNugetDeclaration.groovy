package com.e4d.build

/**
 * Declaration that defines nuget keyword that can by used inside publish block.
 */
class PublishNugetDeclaration {
  final List<Deployer> _deployers

  /**
   * Constructor
   *
   * @param deployers A list of objects that will be populated by
   *                  specific deployers.
   */
  PublishNugetDeclaration(List<Deployer> deployers) {
    _deployers = deployers
  }

  PublishNugetDeclaration() {
    _deployers = new ArrayList<Deployer>()
  }

  def getDeployers() {
    _deployers.asImmutable()
  }

  /**
   * Defines csproject keyword.
   * Adds deployer that publishes specified C# project.
   *
   * @param project A string denotes the C# project.
   * @param options A map of arbitrary options.
   */
  void csproject(Map options=[:], String project) {
    _deployers.add(new CSProjectNugetPublisher(options + [project: project]))
  }

  /**
   * Defines nupackage keyword.
   * Adds deployer that publishes specified nuget package.
   *
   * @param pkg A string denotes the nuget package.
   * @param options A map of arbitrary options.
   */
  void nupackage(Map options=[:], String pkg) {
    _deployers.add(new NuPackageNugetPublisher(pkg: pkg))
  }
}
