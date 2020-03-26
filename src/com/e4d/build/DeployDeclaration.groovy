package com.e4d.build

/**
 * Declaration that defines deploy keyword.
 */
class DeployDeclaration {
  final List<Deployer> _deployers

  /**
   * Constructor
   *
   * @param deployers A list of objects that will be populated by
   *                  specific deployers.
   */
  DeployDeclaration(List<Deployer> deployers) {
    _deployers = deployers
  }

  DeployDeclaration() {
    _deployers = new ArrayList<Deployer>()
  }

  def getDeployers() {
    _deployers.asImmutable()
  }

  /**
   * Defines publish keyword.
   *
   * @param code A blosk of code that gets interpreted by
   *             {@code @PublishDeclaration}.
   */
  def publish(Closure code) {
    code.delegate = new PublishDeclaration(_deployers)
    code()
  }
}
