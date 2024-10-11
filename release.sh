#!/bin/bash

# Script to perform a release of dataverse. The next development version depends on the script argument.
# For instance, if the current version in pom.xml is 1.0.1-SNAPSHOT a git tag will be created with version 1.0.1 and
# development version will be set to:
# - Without any argument => 1.0.2-SNAPSHOT
# - major                => 2.0.0-SNAPSHOT
# - minor                => 1.1.0-SNAPSHOT

set -e

NEXT_DEV_VERSION=$1

parseVersion() {
  EXPRESSION=$1
  ./mvnw build-helper:parse-version help:evaluate -Dexpression=${EXPRESSION} -q -DforceStdout
}

echo "Retrieving version information."

CURRENT_MAJOR_VERSION=$(parseVersion "parsedVersion.majorVersion")
NEXT_MAJOR_VERSION=$(parseVersion "parsedVersion.nextMajorVersion")
CURRENT_MINOR_VERSION=$(parseVersion "parsedVersion.minorVersion")
NEXT_MINOR_VERSION=$(parseVersion "parsedVersion.nextMinorVersion")
CURRENT_PATCH_VERSION=$(parseVersion "parsedVersion.incrementalVersion")
NEXT_PATCH_VERSION=$(parseVersion "parsedVersion.nextIncrementalVersion")

RELEASE_FULL_VERSION="${CURRENT_MAJOR_VERSION}.${CURRENT_MINOR_VERSION}.${CURRENT_PATCH_VERSION}"

if [ "${NEXT_DEV_VERSION}" == "major" ]; then
  DEVELOPMENT_FULL_VERSION="${NEXT_MAJOR_VERSION}.0.0-SNAPSHOT"
elif [ "${NEXT_DEV_VERSION}" == "minor" ]; then
  DEVELOPMENT_FULL_VERSION="${CURRENT_MAJOR_VERSION}.${NEXT_MINOR_VERSION}.0-SNAPSHOT"
else
  DEVELOPMENT_FULL_VERSION="${CURRENT_MAJOR_VERSION}.${CURRENT_MINOR_VERSION}.${NEXT_PATCH_VERSION}-SNAPSHOT"
fi

echo "Version to be released: ${RELEASE_FULL_VERSION}. Next development version will be set to: ${DEVELOPMENT_FULL_VERSION}."

./mvnw --batch-mode release:prepare release:perform -s settings.xml \
    -Darguments="-DskipTests -Ddocker.skip" \
    -DignoreSnapshots=true -DautoVersionSubmodules=true \
    -DreleaseVersion=${RELEASE_FULL_VERSION} \
    -DdevelopmentVersion=${DEVELOPMENT_FULL_VERSION}
