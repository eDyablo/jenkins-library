#!/usr/bin/env bash

# Runs tests from .net projects from specified directory
#
# @param BASE_DIR is a path to direcory where project files reside
# @param BUILD_CONFIGURATION is a name of configuration to use for build
# @param EXCLUDE_PROJECT_PATTERN is a regex pattern specifies what projects must not be build
# @param INCLUDE_PROJECT_PATTERN is a regex pattern specifies what projects build
# @param TEST_FILTER is an expression specifies what tests must be included or excluded for run
# @param RESULTS_DIR is a name of subdirectory where to store results of the test run
set -o errexit
baseDir=$(realpath $BASE_DIR)
projects=$(find "$baseDir" -regex "$INCLUDE_PROJECT_PATTERN" \
  -and -not -regex "$EXCLUDE_PROJECT_PATTERN")
resultsDir=$(realpath $RESULTS_DIR)
mkdir --parents --mode=777 "$resultsDir"
for project in $projects
do
  projectDir=$(dirname "$project")
  testResultFileName=test_result_$(date +%Y%m%d_%H%M%S).trx
  dotnet test "$project" --no-restore --no-build \
    ${BUILD_CONFIGURATION:+ --configuration "$BUILD_CONFIGURATION"} \
    ${TEST_FILTER:+ --filter "$TEST_FILTER"} \
    --results-directory "$resultsDir" \
    --logger "trx;LogFileName=$testResultFileName" \
    /p:CollectCoverage=true \
    /p:CoverletOutputFormat=\"json,opencover,cobertura\" \
    /p:CoverletOutput="$resultsDir/" \
    /p:ThresholdType=line
done
