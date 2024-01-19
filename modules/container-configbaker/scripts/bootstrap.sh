#!/bin/bash

# [INFO]: Execute bootstrapping configuration of a freshly baked instance

set -euo pipefail

function usage() {
  echo "Usage: $(basename "$0") [-h] [-u instanceUrl] [-t timeout] [-e targetEnvFile] [<persona>]"
  echo ""
  echo "Execute initial configuration (bootstrapping) of an empty Dataverse instance."
  echo -n "Known personas: "
  find "${BOOTSTRAP_DIR}" -mindepth 1 -maxdepth 1 -type d -exec basename {} \; | paste -sd ' '
  echo ""
  echo "Parameters:"
  echo "  instanceUrl - Location on container network where to reach your instance. Default: 'http://dataverse:8080'"
  echo "      timeout - Provide how long to wait for the instance to become available (using wait4x). Default: '2m'"
  echo "targetEnvFile - Path to a file where the bootstrap process can expose information as env vars (e.g. dataverseAdmin's API token)"
  echo "      persona - Configure persona to execute. Calls ${BOOTSTRAP_DIR}/<persona>/init.sh. Default: 'base'"
  echo ""
  echo "Note: This script will wait for the Dataverse instance to be available before executing the bootstrapping."
  echo "      It also checks if already bootstrapped before (availability of metadata blocks) and skip if true."
  echo ""
  exit 1
}

# Set some defaults as documented
DATAVERSE_URL=${DATAVERSE_URL:-"http://dataverse:8080"}
TIMEOUT=${TIMEOUT:-"3m"}
TARGET_ENV_FILE=${TARGET_ENV_FILE:-""}

while getopts "u:t:e:h" OPTION
do
  case "$OPTION" in
    u) DATAVERSE_URL="$OPTARG" ;;
    t) TIMEOUT="$OPTARG" ;;
    e) TARGET_ENV_FILE="$OPTARG" ;;
    h) usage;;
    \?) usage;;
  esac
done
shift $((OPTIND-1))

# Assign persona if present or go default
PERSONA=${1:-"base"}

# Export the URL to be reused in the actual setup scripts
export DATAVERSE_URL

# Wait for the instance to become available
echo "Waiting for ${DATAVERSE_URL} to become ready in max ${TIMEOUT}."
wait4x http "${DATAVERSE_URL}/api/info/version" -i 8s -t "$TIMEOUT" --expect-status-code 200 --expect-body-json data.version

# Avoid bootstrapping again by checking if a metadata block has been loaded
BLOCK_COUNT=$(curl -sSf "${DATAVERSE_URL}/api/metadatablocks" | jq ".data | length")
if [[ $BLOCK_COUNT -gt 0 ]]; then
  echo "Your instance has been bootstrapped already, skipping."
  exit 0
fi

# Provide a space to store environment variables output to
ENV_OUT=$(mktemp)
export ENV_OUT

# Now execute the bootstrapping script
echo "Now executing bootstrapping script at ${BOOTSTRAP_DIR}/${PERSONA}/init.sh."
# shellcheck disable=SC1090
source "${BOOTSTRAP_DIR}/${PERSONA}/init.sh"

# If the env file option was given, check if the file is writeable and copy content from the temporary file
if [[ -n "${TARGET_ENV_FILE}" ]]; then
  if [[ -f "${TARGET_ENV_FILE}" && -w "${TARGET_ENV_FILE}" ]]; then
    cat "${ENV_OUT}" > "${TARGET_ENV_FILE}"
  else
    echo "File ${TARGET_ENV_FILE} not found, is a directory or not writeable"
    exit 2
  fi
fi
