#!/usr/bin/env bash

# [INFO]: Idempotent replacement of all database settings from a file source.

set -euo pipefail

function usage() {
  echo "Usage: $(basename "$0") [-h] [-u instanceUrl] [-t timeout] [-c configFile] [-b unblockKey] [-e envSource]"
  echo ""
  echo "Replace all Database Settings in a running Dataverse installation in an idempotent way."
  echo ""
  echo "Parameters:"
  echo "instanceUrl - Location on container network where to reach your instance. Default: 'http://dataverse:8080'"
  echo "              Can be set as environment variable 'DATAVERSE_URL'."
  echo "    timeout - Provide how long to wait for the instance to become available (using wait4x). Default: '3m'"
  echo "              Can be set as environment variable 'TIMEOUT'."
  echo " configFile - Path to a JSON, YAML, PROPERTIES or TOML file containing your settings. Default: '/dv/db-opts.yml'"
  echo "              Can be set as environment variable 'CONFIG_FILE'. May contain \${var} references to env. vars."
  echo " unblockKey - Either string or path to a file with the Admin API Unblock Key. Optional for localhost. No default."
  echo "              Can be set as environment variable 'ADMIN_API_UNBLOCK_KEY'."
  echo "  envSource - Path to a file or directory used as source for additional environment variables."
  echo "              Optional, no default. Can be set as environment variable 'ENV_SOURCE'."
  echo "              Environment variables from this file or directory structure will be script-local."
  echo ""
  echo "Note: This script will wait for the Dataverse instance to be available before executing the replacement."
  echo "      Be careful - this script will not stop you from deleting any vital settings."
  echo ""
  exit 1
}

source util/common.sh
source util/read-to-env.sh

# Check for (the right) yq, jq, and wait4x being available
require_on_path yq
if ! grep -q "https://github.com/mikefarah/yq" <((yq --version)); then
  error "You must install yq from https://github.com/mikefarah/yq, not https://github.com/kislyuk/yq"
fi
require_on_path jq
require_on_path wait4x

# Set some defaults as documented
DATAVERSE_URL=${DATAVERSE_URL:-"http://dataverse:8080"}
ADMIN_API_UNBLOCK_KEY=${ADMIN_API_UNBLOCK_KEY:-""}
TIMEOUT=${TIMEOUT:-"3m"}
CONFIG_FILE=${CONFIG_FILE:-"/dv/db-opts.yml"}
ENV_SOURCE=${ENV_SOURCE:-""}

while getopts "u:t:c:b:e:h" OPTION
do
  case "$OPTION" in
    u) DATAVERSE_URL="$OPTARG" ;;
    t) TIMEOUT="$OPTARG" ;;
    c) CONFIG_FILE="$OPTARG" ;;
    b) ADMIN_API_UNBLOCK_KEY="$OPTARG" ;;
    e) ENV_SOURCE="$OPTARG" ;;
    h) usage;;
    \?) usage;;
  esac
done
shift $((OPTIND-1))

#####   #####   #####   #####   #####   #####   #####   #####   #####   #####   #####   #####   #####   #####   #####
# PARSE CONFIGURATION

# In case the env source was given as cmd arg, parse it
if [ -n "$ENV_SOURCE" ]; then
  read_to_env "$ENV_SOURCE"
fi

# Check for file with DB options given, file present and readable as well as parseable by yq
# If parseable, render as JSON to temp file
CONV_CONF_FILE=$(mktemp)
if [ -f "${CONFIG_FILE}" ] && [ -r "${CONFIG_FILE}" ]; then
  # See https://mikefarah.gitbook.io/yq/operators/env-variable-operators#tip
  yq -M -o json '(.. | select(tag == "!!str")) |= envsubst(nu)' "${CONFIG_FILE}" > "${CONV_CONF_FILE}" || error "Could not parse config file with yq from '${CONFIG_FILE}'."
  # TODO: think about adding a debug switch here, not just print
  # cat "$CONV_CONF_FILE"
else
  error "Could not read a config file at '${CONFIG_FILE}'."
fi

#####   #####   #####   #####   #####   #####   #####   #####   #####   #####   #####   #####   #####   #####   #####
# API INTERACTION

# Define an auth header argument (enabling usage of different ways)
AUTH_HEADER_ARG=""

# Check for Dataverse Unblock API Key present (option with file/env var)
# This is only required if the host is not localhost (then there may be no key necessary)
if ! [[ "${DATAVERSE_URL}" == *"://localhost"* ]] || [ -n "${ADMIN_API_UNBLOCK_KEY}" ]; then
  # The argument should not be empty
  if [ -z "${ADMIN_API_UNBLOCK_KEY}" ]; then
    error "You must provide the Dataverse API Unblock Key to this script."
  # In case it's not empty, check if it's a file path and read the key from there
  elif [ -f "${ADMIN_API_UNBLOCK_KEY}" ] && [ -r "${ADMIN_API_UNBLOCK_KEY}" ]; then
    echo "Reading Dataverse API Unblock Key from ${ADMIN_API_UNBLOCK_KEY}."
    if ! API_KEY_FILE_CONTENT=$(cat "${ADMIN_API_UNBLOCK_KEY}" 2>/dev/null); then
      error "Could not read unblock key from file ${ADMIN_API_UNBLOCK_KEY}."
    fi
    # Validate the key is not empty
    if [ -z "${API_KEY_FILE_CONTENT}" ]; then
      error "API key file ${ADMIN_API_UNBLOCK_KEY} appears empty."
    fi
    ADMIN_API_UNBLOCK_KEY="$API_KEY_CONTENT"
  fi
  # Very basic error check (as there is no clear format or formal spec for the key)
  if [ ${#ADMIN_API_UNBLOCK_KEY} -lt 5 ]; then
    error "API key appears to be too short (<5 chars)."
  fi

  # Build the header argument for Admin API Authentication via unblock key
  AUTH_HEADER_ARG="X-Dataverse-unblock-key: ${ADMIN_API_UNBLOCK_KEY}"
fi

# Check or wait for Dataverse API being responsive
echo "Waiting for ${DATAVERSE_URL} to become ready in max ${TIMEOUT}."
wait4x http "${DATAVERSE_URL}/api/info/version" -i 8s -t "$TIMEOUT" --expect-status-code 200 --expect-body-json data.version

# Check for Dataverse Admin API endpoints being reachable by retrieving the current DB options, expect blockades!
CURRENT_SETTINGS=$(mktemp)
echo "Retrieving settings from running instance."
# TODO: Do we need to support pre v6.7 style unblock key query parameter?
curl -sSL --fail-with-body -o "${CURRENT_SETTINGS}" -H "${AUTH_HEADER_ARG}" "${DATAVERSE_URL}/api/admin/settings" \
  || error "Failed. Response message: $( cat "${CURRENT_SETTINGS}")" \
  && echo "Success!"
  # TODO: while it's nice to have the current settings written out, it may contain sensitive information (so don't).
  # && ( echo "Success! Current settings: "; jq '.data' < "$CURRENT_SETTINGS" )

# We need to make the settings update atomic.
echo "Replacing settings."
RESPONSE=$(mktemp)
curl -sSL --fail-with-body -o "${RESPONSE}" -X PUT -H "${AUTH_HEADER_ARG}" --json @"${CONV_CONF_FILE}" "${DATAVERSE_URL}/api/admin/settings" \
  || error "Failed. Response message: $( jq ".message" < "${RESPONSE}" )" \
  && ( echo -e "Success!\nOperations executed: "; jq '.data' < "$RESPONSE" )
