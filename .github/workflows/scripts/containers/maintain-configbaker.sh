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
# Optional:
# - Use DRY_RUN=1 env var to skip actually building, but see how the tag lookups play out
# - Use DAMP_RUN=1 env var to skip pushing images, but build them

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
DRY_RUN="${DRY_RUN:-"0"}"
DAMP_RUN="${DAMP_RUN:-"0"}"
PLATFORMS="${PLATFORMS:-"linux/amd64,linux/arm64"}"

# Setup and validation
if [[ -z "$*" ]]; then
  >&2 echo "You must give a list of branch names as arguments"
  exit 1;
fi

if (( DRY_RUN + DAMP_RUN > 1 )); then
  >&2 echo "You must either use DRY_RUN=1 or DAMP_RUN=1, but not both"
  exit 1;
fi

source "$( dirname "$0" )/utils.sh"

# Delete old stuff if present
rm -rf "$MAINTENANCE_WORKSPACE"
mkdir -p "$MAINTENANCE_WORKSPACE"

# Store the image tags we maintain in this array (same order as branches array!)
# This list will be used to build the support matrix within the Docker Hub image description
SUPPORTED_ROLLING_TAGS=()
# Store the tags of config baker images we are actually rebuilding
# Takes the from "branch-name=config-image-ref"
REBUILT_CONFIG_IMAGES=()

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
  git clone -c advice.detachedHead=false --depth=1 --branch "$BRANCH" "${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}" "$MAINTENANCE_WORKSPACE/$BRANCH"
  # Switch context
  cd "$MAINTENANCE_WORKSPACE/$BRANCH"

  # 2. Now let's apply the patches (we have them checked out in $GITHUB_WORKSPACE, not necessarily in this local checkout)
  echo "Checking for patches..."
  if [[ -d ${GITHUB_WORKSPACE}/modules/container-configbaker/backports/$BRANCH ]]; then
    echo "Applying patches now."
    find "${GITHUB_WORKSPACE}/modules/container-configbaker/backports/$BRANCH" -type f -name '*.patch' -print0 | xargs -0 -n1 patch -p1 -l -s -i
  fi

  # 3a. Determine the base image ref (<namespace>/<repo>:<tag>)
  BASE_IMAGE_REF=$( mvn initialize help:evaluate -Pct -f . -Dexpression=conf.image.base -q -DforceStdout )
  echo "Determined BASE_IMAGE_REF=$BASE_IMAGE_REF from Maven"

  # 3b. Determine the configbaker image ref (<namespace>/<repo>:<tag>)
  CONFIG_IMAGE_REF=""
  if (( IS_DEV )); then
    # Results in the rolling tag for the dev branch
    CONFIG_IMAGE_REF=$( mvn initialize help:evaluate -Pct -f . -Dexpression=conf.image -q -DforceStdout )
  else
    # Results in the rolling tag for the release branch (the fixed tag will be determined from this rolling tag)
    # shellcheck disable=SC2016
    CONFIG_IMAGE_REF=$( mvn initialize help:evaluate -Pct -f . -Dexpression=conf.image -Dconf.image.tag='${app.image.version}-${conf.image.flavor}' -q -DforceStdout )
  fi
  echo "Determined CONFIG_IMAGE_REF=$CONFIG_IMAGE_REF from Maven"

  # 4a. Check for Base image updates
  NEWER_BASE_IMAGE=0
  if check_newer_parent "$BASE_IMAGE_REF" "$CONFIG_IMAGE_REF"; then
    NEWER_BASE_IMAGE=1
  fi

  # 4b. Check for vulnerabilities in packages fixable by updating
  FIXES_AVAILABLE=0
  if ! (( NEWER_BASE_IMAGE )) && check_trivy_fixes_for_os "$CONFIG_IMAGE_REF"; then
    FIXES_AVAILABLE=1
  fi

  # 5. Get current immutable revision tag if not on the dev branch
  REV=$( current_revision "$CONFIG_IMAGE_REF" )
  CURRENT_REV_TAG="${CONFIG_IMAGE_REF#*:}-r$REV"
  NEXT_REV_TAG="${CONFIG_IMAGE_REF#*:}-r$(( REV + 1 ))"

  # 6. Let's put together what tags we want added to this build run
  TAG_OPTIONS=""
  if ! (( IS_DEV )); then
    TAG_OPTIONS="-Dconf.image=$CONFIG_IMAGE_REF -Ddocker.tags.revision=$NEXT_REV_TAG"
    # In case of the current release, add the "latest" tag as well.
    if (( IS_CURRENT_RELEASE )); then
      TAG_OPTIONS="$TAG_OPTIONS -Ddocker.tags.latest=latest"
    fi
  else
    # shellcheck disable=SC2016
    UPCOMING_TAG=$( mvn initialize help:evaluate -Pct -f . -Dexpression=conf.image.tag -Dconf.image.tag='${app.image.version}-${conf.image.flavor}' -q -DforceStdout )
    TAG_OPTIONS="-Ddocker.tags.upcoming=$UPCOMING_TAG"

    # For the dev branch we only have rolling tags and can add them now already
    SUPPORTED_ROLLING_TAGS+=("[\"unstable\", \"$UPCOMING_TAG\"]")
  fi
  echo "Determined these additional Maven tag options: $TAG_OPTIONS"

  # 8. Let's build the base image if necessary
  NEWER_IMAGE=0
  if (( NEWER_BASE_IMAGE + FIXES_AVAILABLE + FORCE_BUILD > 0 )); then
    if ! (( DRY_RUN )); then
      # Build the application image, but skip the configbaker image (that's a different job)!
      # shellcheck disable=SC2046
      mvn -Pct -f . deploy -Ddocker.noCache -Ddocker.platforms="${PLATFORMS}" \
        -Dapp.skipBuild -Dconf.image.base="${BASE_IMAGE_REF}" \
        -Dmaven.main.skip -Dmaven.test.skip -Dmaven.war.skip \
        -Ddocker.imagePropertyConfiguration=override $TAG_OPTIONS \
        $( if (( DAMP_RUN )); then echo "-Ddocker.skip.push"; fi )
    else
      echo "Skipping Maven build as requested by DRY_RUN=1"
    fi
    NEWER_IMAGE=1
    # Save the information about the immutable or rolling tag we just built
    if ! (( IS_DEV )); then
      REBUILT_CONFIG_IMAGES+=("$BRANCH=${CONFIG_IMAGE_REF%:*}:$NEXT_REV_TAG")
    else
      REBUILT_CONFIG_IMAGES+=("$BRANCH=$CONFIG_IMAGE_REF")
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
    RELEASE_TAGS_LIST+="\"${CONFIG_IMAGE_REF#*:}\", "
    if (( NEWER_IMAGE )); then
      RELEASE_TAGS_LIST+="\"$NEXT_REV_TAG\"]"
    else
      RELEASE_TAGS_LIST+="\"$CURRENT_REV_TAG\"]"
    fi
    SUPPORTED_ROLLING_TAGS+=("${RELEASE_TAGS_LIST}")
  fi

  echo "::endgroup::"
done

# Built the output which images have actually been rebuilt as JSON
REBUILT_IMAGES="["
for IMAGE in "${REBUILT_CONFIG_IMAGES[@]}"; do
  REBUILT_IMAGES+=" \"$IMAGE\" "
done
REBUILT_IMAGES+="]"
echo "rebuilt_images=${REBUILT_IMAGES//  /, }" | tee -a "${GITHUB_OUTPUT}"

# Built the supported rolling tags matrix as JSON
SUPPORTED_TAGS="{"
for (( i=0; i < ${#SUPPORTED_ROLLING_TAGS[@]} ; i++ )); do
  j=$((i+1))
  SUPPORTED_TAGS+="\"${!j}\": ${SUPPORTED_ROLLING_TAGS[$i]}"
  (( i < ${#SUPPORTED_ROLLING_TAGS[@]}-1 )) && SUPPORTED_TAGS+=", "
done
SUPPORTED_TAGS+="}"
echo "supported_tag_matrix=$SUPPORTED_TAGS" | tee -a "$GITHUB_OUTPUT"
