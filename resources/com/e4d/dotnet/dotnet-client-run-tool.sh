#!/usr/bin/env bash

# Runs specified tool for .net projects from specified directory
#
# @param BASE_DIR is a path to direcory where project files reside
# @param EXCLUDE_PATTERN is a regex pattern specifies what projects must not be build
# @param INCLUDE_PATTERN is a regex pattern specifies what projects build
# @param NUGET_CONFIG is a configuration for nuget package manager
# @param TOOL is a name of the tool
# @param COMMAND is a command for the tool
# @param COMMAND_ARGUMENTS is a list of arguments for the command
# @param COMMAND_OPTIONS is a list of options for the command
{
  baseDir=`realpath "$BASE_DIR"`
  echo $NUGET_CONFIG > "$baseDir/nuget.config"
  projects=`find "$baseDir" -regex "$INCLUDE_PATTERN" \
      -and -not -regex "$EXCLUDE_PATTERN"`
} 2> /dev/null
for project in $projects
do
  {
    projectDir=`dirname $project`
  } 2> /dev/null
  dotnet restore "$projectDir" --configfile "$baseDir/nuget.config"
  {
    dir=`pwd`
    cd "$projectDir"
  } 2> /dev/null
  dotnet $TOOL $COMMAND $COMMAND_ARGUMENTS $COMMAND_OPTIONS
  {
    cd $dir
  } 2> /dev/null
done
