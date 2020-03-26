package com.e4d.build

/**
 * Declaration that defines publish keyword.
 */
class PublishDeclaration {
  final List<Deployer> _deployers

  /**
   * Constructor
   *
   * @param deployers A list of objects that will be populated by
   *                  specific deployers.
   */
  PublishDeclaration(List<Deployer> deployers) {
    _deployers = deployers
  }

  PublishDeclaration() {
    _deployers = new ArrayList<Deployer>()
  }

  def getDeployers() {
    _deployers.asImmutable()
  }

  /**
   * Defines nuget keyword.
   *
   * @param code A blosk of code that gets interpreted by
   *             {@code @PublishNugetDeclaration}.
   */
  def nuget(Closure code) {
    code.delegate = new PublishNugetDeclaration(_deployers)
    code()
  }
}
