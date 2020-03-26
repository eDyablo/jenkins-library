package com.e4d.build

import com.e4d.dotnet.DotnetTool

/**
 * Deployer that publishes a nuget package.
 */
class NuPackageNugetPublisher implements Deployer {
  String pkg
  String server
  String apiKey

  /**
   * See {@code @Deployer}.
   */
  boolean canDeploy(context) {
    true
  }

  /**
   * Pushes nuget package specified by its name to the repository specified by
   * the server and the apiKey properties.
   * See {@code @Deployer}.
   */
  def deploy(context) {
    def dotnet = createDotnetTool(context)
    dotnet.nugetPush(pkg,
      source: server ?: context?.nuget?.server,
      api_key: apiKey ?: context?.nuget?.api_key,
    )
  }

  protected def createDotnetTool(context) {
    new DotnetTool(context.pipeline, context.shell)
  }
}
