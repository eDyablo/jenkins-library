package com.e4d.build

import com.e4d.dotnet.DotnetTool

import org.junit.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*
import static org.mockito.Mockito.any
import static com.e4d.mockito.Matchers.*

class NuPackageNugetPublisherTest extends NuPackageNugetPublisher {
  final DotnetTool dotnet = mock(DotnetTool.class)
  final static def emptyContext = [:]

  @Override def createDotnetTool(context) {
    dotnet
  }

  @Test void deploy_pushes_specified_nuget_package() {
    pkg = 'package'
    deploy(emptyContext)
    verify(dotnet).nugetPush(any(Map.class), eq('package'))
  }

  @Test void deploy_pushes_nuget_to_specified_server() {
    server = 'server'
    deploy(emptyContext)
    verify(dotnet).nugetPush(mapContains(source: 'server'), anyObject())
  }

  @Test void deploy_pushes_nuget_using_server_from_context_when_no_server_specified() {
    server = null
    deploy([nuget: [server: 'server from context']])
    verify(dotnet).nugetPush(mapContains(source: 'server from context'), anyObject())
  }

  @Test void deploy_pushes_nuget_preffering_specified_server_over_nuget_server_from_context() {
    server = 'server'
    deploy([nuget: [server: 'server from context']])
    verify(dotnet).nugetPush(mapContains(source: 'server'), anyObject())
  }

  @Test void deploy_pushes_nuget_with_specified_api_key() {
    apiKey = 'key'
    deploy(emptyContext)
    verify(dotnet).nugetPush(mapContains(api_key: 'key'), anyObject())
  }

  @Test void deploy_pushes_nuget_using_nuget_api_key_from_context_when_no_api_key_specified() {
    apiKey = null
    deploy([nuget: [api_key: 'key from context']])
    verify(dotnet).nugetPush(mapContains(api_key: 'key from context'), anyObject())
  }

  @Test void deploy_pushes_nuget_preffering_specified_api_key_over_nuget_api_key_from_context() {
    apiKey = 'key'
    deploy([nuget: [api_key: 'key from context']])
    verify(dotnet).nugetPush(mapContains(api_key: 'key'), anyObject())
  }
}
