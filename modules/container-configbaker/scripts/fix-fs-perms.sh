#!/bin/bash

# [INFO]: Fix folder permissions using 'chown' to be writeable by containers not running as root.

set -euo pipefail

if [[ "$(id -un)" != "root" ]]; then
  echo "This script must be run as user root (not $(id -un)), otherwise no fix is possible."
fi

DEF_DV_PATH="/dv"
DEF_SOLR_PATH="/var/solr"
DEF_DV_UID="1000"
DEF_SOLR_UID="8983"

function usage() {
  echo "Usage: $(basename "$0") (dv|solr|[1-9][0-9]{3,4}) [PATH [PATH [...]]]"
  echo ""
  echo "You may omit a path when using 'dv' or 'solr' as first argument:"
  echo "  - 'dv' will default to user $DEF_DV_UID and $DEF_DV_PATH"
  echo "  - 'solr' will default to user $DEF_SOLR_UID and $DEF_SOLR_PATH"
  exit 1
}

# Get a target name or id
TARGET=${1:-help}
# Get the rest of the arguments as paths to apply the fix to
PATHS=( "${@:2}" )

ID=0
case "$TARGET" in
  dv)
    ID="$DEF_DV_UID"
    # If there is no path, add the default for our app image
    if [[ ${#PATHS[@]} -eq 0 ]]; then
      PATHS=( "$DEF_DV_PATH" )
    fi
    ;;
  solr)
    ID="$DEF_SOLR_UID"
    # In case there is no path, add the default path for Solr images
    if [[ ${#PATHS[@]} -eq 0 ]]; then
      PATHS=( "$DEF_SOLR_PATH" )
    fi
    ;;
  # If there is a digit in the argument, check if this is a valid UID (>= 1000, ...)
  *[[:digit:]]* )
    echo "$TARGET" | grep -q "^[1-9][0-9]\{3,4\}$" || usage
    ID="$TARGET"
    ;;
  *)
    usage
    ;;
esac

# Check that we actually have at least 1 path
if [[ ${#PATHS[@]} -eq 0 ]]; then
  usage
fi

# Do what we came for
chown -R "$ID:$ID" "${PATHS[@]}"
