#!/bin/bash
################################################################################
# This script is used to configure a Dataverse installation from a ConfigMap.
# It is used solely for changing Database settings!
################################################################################

# Fail on any error
set -euo pipefail
DATAVERSE_SERVICE_HOST=${DATAVERSE_SERVICE_HOST:-"dataverse"}
DATAVERSE_SERVICE_PORT_HTTP=${DATAVERSE_SERVICE_PORT_HTTP:-"8080"}
DATAVERSE_URL=${DATAVERSE_URL:-"http://${DATAVERSE_SERVICE_HOST}:${DATAVERSE_SERVICE_PORT_HTTP}"}

echo -e "\nRunning configuration job for Dataverse at ${DATAVERSE_URL}."

# Check API key secret is available
if [ ! -s "${SECRETS_DIR}/api/key" ]; then
  echo "No API key present. Failing."
  exit 126
fi
API_KEY=`cat ${SECRETS_DIR}/api/key`

# Set Database options based on environment variables db_XXX from ConfigMap
echo "Setting Database options:"
if `env | grep -Ee '^db_' 2>&1 > /dev/null`; then
  env -0 | grep -z -Ee "^db_" | while IFS='=' read -r -d '' k v; do
      KEY=`echo "${k}" | sed -e 's/^db_/:/'`
      echo -n "Handling ${KEY}=${v}."
      if [[ -z "${v}" ]]; then
        # empty var => delete the setting
        echo -n " Deleting... "
        OUTPUT=`curl -sSf -X DELETE "${DATAVERSE_URL}/api/admin/settings/${KEY}?unblock-key=${API_KEY}" 2>&1 || echo -n ""`
        echo "$OUTPUT" | jq -rM '.status' 2>/dev/null || echo -e 'FAILED\n' "$OUTPUT"
      else
        # set the setting
        echo -n " Setting... "
        OUTPUT=`curl -sSf -X PUT -d "${v}" "${DATAVERSE_URL}/api/admin/settings/${KEY}?unblock-key=${API_KEY}" 2>&1 || echo -n ""`
        echo "$OUTPUT" | jq -rM '.status' 2>/dev/null || echo -e 'FAILED\n' "$OUTPUT"
      fi
  done
else
  echo "--- none found ---"
fi

# Parse and configure authentication providers
echo "Deploying authentication providers:"
if [ -n "${AUTH_PROVIDERS+x}" ]; then
  # iterate all providers in array
  for k in $(echo "${AUTH_PROVIDERS}" | jq '. | keys | .[]'); do
    # get provider element, do nice logging and create tempfile
    PROVIDER=`echo "$AUTH_PROVIDERS" | jq -r ".[$k]"`
    echo -n "Loading `echo "${PROVIDER}" | jq -r ".id"`: "
    TMPFILE=`mktemp`

    # templating magic with esh
    echo "${PROVIDER}" | esh - > "${TMPFILE}"

    # upload with nice logging
    OUTPUT=`curl -sSf -H "Content-type: application/json" -X POST --upload-file "${TMPFILE}" "${DATAVERSE_URL}/api/admin/authenticationProviders?unblock-key=${API_KEY}" 2>&1 || echo -n ""`
    echo "$OUTPUT" | jq -rM '.status' 2>/dev/null || echo -e 'FAILED\n' "$OUTPUT"

    # cleanup behind us, delete tempfile
    rm "${TMPFILE}"
  done
else
  echo "--- none found ---"
fi
