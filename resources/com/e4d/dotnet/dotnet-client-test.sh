#!/usr/bin/env bash

# Runs tests from .net projects from specified directory
#
# @param BASE_DIR is a path to direcory where project files reside
# @param BUILD_CONFIGURATION is a name of configuration to use for build
# @param EXCLUDE_PROJECT_PATTERN is a regex pattern specifies what projects must not be build
# @param INCLUDE_PROJECT_PATTERN is a regex pattern specifies what projects build
# @param NUGET_CONFIG is a configuration for nuget package manager
# @param TEST_FILTER is an expression specifies what tests must be included or excluded for run
# @param TEST_RESULTS_DIR_NAME is a name of subdirectory where to store results of the test run
{
  baseDir=`realpath $BASE_DIR`
  projects=`find "$baseDir" -regex "$INCLUDE_PROJECT_PATTERN" \
      -and -not -regex "$EXCLUDE_PROJECT_PATTERN"`
  resultsDir=$baseDir/$TEST_RESULTS_DIR_NAME
  echo $NUGET_CONFIG > $baseDir/nuget.config
  mkdir --parents --mode=777 "$resultsDir"
} 2> /dev/null
for project in $projects
do
  {
    projectDir=`dirname "$project"`
    testResultFileName=test_result_`date +%Y%m%d_%H%M%S`.trx
  } 2> /dev/null
  dotnet restore "$projectDir" --configfile "$baseDir/nuget.config"
  dotnet test "$project" --no-restore \
      ${BUILD_CONFIGURATION:+ --configuration "$BUILD_CONFIGURATION"} \
      ${TEST_FILTER:+ --filter "$TEST_FILTER"} \
      --results-directory "$resultsDir" \
      --logger "trx;LogFileName=$testResultFileName"
done
