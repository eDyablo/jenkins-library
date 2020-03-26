#!/usr/bin/env bash

# Bilds .net projects from specified directory
#
# @param BASE_DIR is a path to direcory where project files reside
# @param BUILD_CONFIGURATION is a name of configuration to use for build
# @param EXCLUDE_PATTERN is a regex pattern specifies what projects must not be build
# @param INCLUDE_PATTERN is a regex pattern specifies what projects build
# @param NUGET_CONFIG is a configuration for nuget package manager
{
  baseDir=`realpath $BASE_DIR`
  echo $NUGET_CONFIG > $baseDir/nuget.config
  projects=`find "$baseDir" -regex "$INCLUDE_PATTERN" \
      -and -not -regex "$EXCLUDE_PATTERN"`
} 2> /dev/null
for project in $projects
do
  {
    projectDir=`dirname $project`
  } 2> /dev/null
  dotnet restore "$projectDir" --configfile "$baseDir/nuget.config"
  dotnet build "$project" --no-restore \
      ${BUILD_CONFIGURATION:+ --configuration "$BUILD_CONFIGURATION"}
done