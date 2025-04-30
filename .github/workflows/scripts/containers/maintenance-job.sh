#!/bin/bash

# A matrix-like job to maintain a number of releases as well as the latest snap of Dataverse.

# PREREQUISITES:
# - You have Java, Maven, QEMU and Docker all setup and ready to go
# - You obviously checked out the develop branch, otherwise you'd not be executing this script
# - You added all the branch names you want to run maintenance for as arguments
# Optional, but recommended:
# - You added a DEVELOPMENT_BRANCH env var to your runner/job env with the name of the development branch
# - You added a FORCE_BUILD=0|1 env var to indicate if the base image build should be forced
# - You added a PLATFORMS env var with all the target platforms you want to build for

# NOTE:
# This script is a culmination of Github Action steps into a single script.
# The reason to put all of this in here is due to the complexity of the Github Action and the limitation of the
# matrix support in Github actions, where outputs cannot be aggregated or otherwise used further.

set -euo pipefail

# Get all the inputs
# If not within a runner, just print to stdout (duplicating the output in case of tee usage, but that's ok for testing)
GITHUB_OUTPUT=${GITHUB_OUTPUT:-"/proc/self/fd/1"}
GITHUB_ENV=${GITHUB_ENV:-"/proc/self/fd/1"}
GITHUB_WORKSPACE=${GITHUB_WORKSPACE:-"$(pwd)"}
GITHUB_SERVER_URL=${GITHUB_SERVER_URL:-"https://github.com"}
GITHUB_REPOSITORY=${GITHUB_REPOSITORY:-"IQSS/dataverse"}

MAINTENANCE_WORKSPACE="${GITHUB_WORKSPACE}/maintenance-job"

DEVELOPMENT_BRANCH="${DEVELOPMENT_BRANCH:-"develop"}"
FORCE_BUILD="${FORCE_BUILD:-"0"}"
PLATFORMS="${PLATFORMS:-"linux/amd64,linux/arm64"}"

# Setup and validation
if [[ -z "$*" ]]; then
  >&2 echo "You must give a list of branch names as arguments"
  exit 1;
fi

source "$( dirname "$0" )/utils.sh"

# Delete old stuff if present
rm -rf "$MAINTENANCE_WORKSPACE"
mkdir -p "$MAINTENANCE_WORKSPACE"

# Store the image tags we maintain in this array (same order as branches array!)
# This list will be used to build the support matrix within the Docker Hub image description
SUPPORTED_ROLLING_TAGS=()
# Store the tags of base images we are actually rebuilding to base new app images upon
# Takes the from "branch-name=base-image-ref"
REBUILT_BASE_IMAGES=()

for BRANCH in "$@"; do
  echo "::group::Running maintenance for $BRANCH"

  # 0. Determine if this is a development branch and the most current release
  IS_DEV=0
  if [[ "$BRANCH" = "$DEVELOPMENT_BRANCH" ]]; then
    IS_DEV=1
  fi
  IS_CURRENT_RELEASE=0
  if [[ "$BRANCH" = $( curl -f -sS "https://api.github.com/repos/$GITHUB_REPOSITORY/releases" | jq -r '.[0].tag_name' ) ]]; then
    IS_CURRENT_RELEASE=1
  fi

  # 1. Let's get the maintained sources
  git clone -c advice.detachedHead=false --depth 1 --branch "$BRANCH" "${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}" "$MAINTENANCE_WORKSPACE/$BRANCH"
  # Switch context
  cd "$MAINTENANCE_WORKSPACE/$BRANCH"

  # 2. Now let's apply the patches (we have them checked out in $GITHUB_WORKSPACE, not necessarily in this local checkout)
  echo "Checking for patches..."
  if [[ -d ${GITHUB_WORKSPACE}/modules/container-base/src/backports/$BRANCH ]]; then
    echo "Applying patches now."
    find "${GITHUB_WORKSPACE}/modules/container-base/src/backports/$BRANCH" -type f -name '*.patch' -print0 | xargs -0 -n1 patch -p1 -s -i
  fi

  # 3. Determine the base image ref (<namespace>/<repo>:<tag>)
  BASE_IMAGE_REF=""
  # For the dev branch we want to full flexi stack tag, to detect stack upgrades requiring new build
  if (( IS_DEV )); then
    BASE_IMAGE_REF=$( mvn initialize help:evaluate -Pct -f modules/container-base -Dexpression=base.image -q -DforceStdout )
  else
    BASE_IMAGE_REF=$( mvn initialize help:evaluate -Pct -f modules/container-base -Dexpression=base.image -Dbase.image.tag.suffix="" -q -DforceStdout )
  fi
  echo "Determined BASE_IMAGE_REF=$BASE_IMAGE_REF from Maven"

  # 4. Check for Temurin image updates
  JAVA_IMAGE_REF=$( mvn help:evaluate -Pct -f modules/container-base -Dexpression=java.image -q -DforceStdout )
  echo "Determined JAVA_IMAGE_REF=$JAVA_IMAGE_REF from Maven"
  NEWER_JAVA_IMAGE=0
  if check_newer_parent "$JAVA_IMAGE_REF" "$BASE_IMAGE_REF"; then
    NEWER_JAVA_IMAGE=1
  fi

  # 5. Check for package updates in base image
  PKGS="$( grep "ARG PKGS" modules/container-base/src/main/docker/Dockerfile | cut -f2 -d= | tr -d '"' )"
  echo "Determined installed packages=\"$PKGS\" from Maven"
  NEWER_PKGS=0
  # Don't bother with package checks if the java image is newer already
  if ! (( NEWER_JAVA_IMAGE )); then
    if check_newer_pkgs "$BASE_IMAGE_REF" "$PKGS"; then
      NEWER_PKGS=1
    fi
  fi

  # 6. Get current immutable revision tag if not on the dev branch
  REV=$( current_revision "$BASE_IMAGE_REF" )
  CURRENT_REV_TAG="${BASE_IMAGE_REF#*:}-r$REV"
  NEXT_REV_TAG="${BASE_IMAGE_REF#*:}-r$(( REV + 1 ))"

  # 7. Let's put together what tags we want added to this build run
  TAG_OPTIONS=""
  if ! (( IS_DEV )); then
    TAG_OPTIONS="-Dbase.image=$BASE_IMAGE_REF -Ddocker.tags.revision=$NEXT_REV_TAG"
    # In case of the current release, add the "latest" tag as well.
    if (( IS_CURRENT_RELEASE )); then
      TAG_OPTIONS="$TAG_OPTIONS -Ddocker.tags.latest=latest"
    fi
  else
    UPCOMING_TAG=$( mvn initialize help:evaluate -Pct -f modules/container-base -Dexpression=base.image.tag -Dbase.image.tag.suffix="" -q -DforceStdout )
    TAG_OPTIONS="-Ddocker.tags.develop=unstable -Ddocker.tags.upcoming=$UPCOMING_TAG"

    # For the dev branch we only have rolling tags and can add them now already
    SUPPORTED_ROLLING_TAGS+=("[\"unstable\", \"$UPCOMING_TAG\", \"${BASE_IMAGE_REF#*:}\"]")
  fi
  echo "Determined these additional Maven tag options: $TAG_OPTIONS"

  # 8. Let's build the base image if necessary
  NEWER_IMAGE=0
  if (( NEWER_JAVA_IMAGE + NEWER_PKGS + FORCE_BUILD > 0 )); then
    mvn -Pct -f modules/container-base deploy -Ddocker.noCache -Ddocker.platforms="${PLATFORMS}" \
      -Ddocker.imagePropertyConfiguration=override $TAG_OPTIONS
    NEWER_IMAGE=1
    # Save the information about the immutable or rolling tag we just built
    if ! (( IS_DEV )); then
      REBUILT_BASE_IMAGES+=("$BRANCH=${BASE_IMAGE_REF%:*}:$NEXT_REV_TAG")
    else
      REBUILT_BASE_IMAGES+=("$BRANCH=$BASE_IMAGE_REF")
    fi
  else
    echo "No rebuild necessary, we're done here."
  fi

  # 9. Add list of rolling and immutable tags for release builds
  if ! (( IS_DEV )); then
    RELEASE_TAGS_LIST="["
    if (( IS_CURRENT_RELEASE )); then
      RELEASE_TAGS_LIST+="\"latest\", "
    fi
    RELEASE_TAGS_LIST+="\"${BASE_IMAGE_REF#*:}\", "
    if (( NEWER_IMAGE )); then
      RELEASE_TAGS_LIST+="\"$NEXT_REV_TAG\"]"
    else
      RELEASE_TAGS_LIST+="\"$CURRENT_REV_TAG\"]"
    fi
    SUPPORTED_ROLLING_TAGS+=("${RELEASE_TAGS_LIST}")
  fi

  echo "::endgroup::"
done

# Built the output which base images have actually been rebuilt as JSON
REBUILT_IMAGES="["
for IMAGE in "${REBUILT_BASE_IMAGES[@]}"; do
  REBUILT_IMAGES+=" \"$IMAGE\" "
done
REBUILT_IMAGES+="]"
echo "rebuilt_base_images=${REBUILT_IMAGES//  /, }" | tee -a "${GITHUB_OUTPUT}"

# Built the supported rolling tags matrix as JSON
SUPPORTED_TAGS="{"
for (( i=0; i < ${#SUPPORTED_ROLLING_TAGS[@]} ; i++ )); do
  j=$((i+1))
  SUPPORTED_TAGS+="\"${!j}\": ${SUPPORTED_ROLLING_TAGS[$i]}"
  (( i < ${#SUPPORTED_ROLLING_TAGS[@]}-1 )) && SUPPORTED_TAGS+=", "
done
SUPPORTED_TAGS+="}"
echo "supported_tag_matrix=$SUPPORTED_TAGS" | tee -a "$GITHUB_OUTPUT"
