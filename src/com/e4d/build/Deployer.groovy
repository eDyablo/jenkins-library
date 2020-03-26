package com.e4d.build

/**
 * Defines intefrace of deployer.
 */
interface Deployer {
  /**
   * Determines when the publisher can deploy.
   *
   * @param context An outter context that contains information used
   *                to dicide whether deploy or not.
   */
  boolean canDeploy(context)

  /**
   * Does deployment.
   *
   * @param context An outter context that can specify server and api key for
   *                nuget repository.
   */
  def deploy(context)
}
