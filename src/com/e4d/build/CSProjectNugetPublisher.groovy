package com.e4d.build

import com.e4d.dotnet.DotnetTool
import com.e4d.build.SemanticVersion
import com.e4d.build.SemanticVersionBuilder

/**
 * Deployer that publishes a nuget package defined by a C# project.
 */
class CSProjectNugetPublisher implements Deployer {
  String project
  String version
  String versionPrefix
  String versionSuffix
  String server
  String apiKey

  /**
   * Determines when the publisher can deploy.
   *
   * @param context An outter context that contains information used
   *                to dicide whether deploy or not.
   */
  boolean canDeploy(context) {
    true
  }

  /**
   * Pushes nuget package made from the project to the repository specified by
   * the server and the apiKey properties.
   * The resulting package will have version specified by the version,
   * versionPrefix and versionSuffix properties.
   *
   * @param context An outter context that can specify server and api key for
   *                nuget repository.
   */
  def deploy(context) {
    final dotnet = createDotnetTool(context)
    final packed = dotnet.csprojPack(project,
      version: getArtifactVersion(context),
      versionPrefix: versionPrefix,
      versionSuffix: versionSuffix,
    )
    if (packed?.nugets) {
      if (context.nugetRepository?.hasNuget(name: project, version: packed.version)) {
        context.pipeline?.echo("WARNING: Nuget package ${ project }:${ packed.version } cannot be published over existing one")
      } else {
        packed.nugets.findAll { nuget ->
          nuget.endsWith('.nupkg') && !nuget.endsWith('.symbols.nupkg')
        }.each { nuget ->
          dotnet.nugetPush(nuget,
            source: server ?: context?.nuget?.server,
            api_key: apiKey ?: context?.nuget?.api_key,
          )
        }
      }
    }
  }

  protected def createDotnetTool(context) {
    new DotnetTool(context.pipeline, context.shell)
  }

  String getArtifactVersion(context) {
    if (version?.trim()) {
      return version
    }
    else {
      final version = context?.artifactVersion ?: SemanticVersion.ZERO
      final prerelease = version.prereleaseIds.join('.')
      final build = version.buildIds.join('.')
      return [version.core, [prerelease, build].findAll().join('.')].findAll().join('-')
    }
  }
}
