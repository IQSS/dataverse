#!/bin/bash

# [INFO]: Update the metadata blocks and Solr schema of a running, bootstrapped instance

set -euo pipefail

function usage() {
  echo "Usage: $(basename "$0") [-h] [-u instanceUrl] [-s solrUrl] [-c solrCore] [-t timeout]"
  echo ""
  echo "Update the standard metadata blocks of a running, already bootstrapped Dataverse instance"
  echo "by reloading the metadata block TSV files, then update the Solr schema to match."
  echo ""
  echo "Parameters:"
  echo "  instanceUrl - Location on container network where to reach your instance. Default: 'http://dataverse:8080'"
  echo "      solrUrl - Location on container network where to reach Solr. Default: 'http://solr:8983'"
  echo "     solrCore - Name of the Solr core to update and reload. Default: 'collection1'"
  echo "      timeout - How long to wait for the instance to become available. Default: '3m'"
  echo ""
  echo "Note: This script is a no-op on an instance that has not been bootstrapped yet (bootstrap.sh"
  echo "      loads the same TSV files anyway). It is idempotent and safe to run on every startup."
  echo ""
  exit 1
}

# Set some defaults as documented
DATAVERSE_URL=${DATAVERSE_URL:-"http://dataverse:8080"}
SOLR_URL=${SOLR_URL:-"http://solr:8983"}
SOLR_CORE=${SOLR_CORE:-"collection1"}
TIMEOUT=${TIMEOUT:-"3m"}

while getopts "u:s:c:t:h" OPTION
do
  case "$OPTION" in
    u) DATAVERSE_URL="$OPTARG" ;;
    s) SOLR_URL="$OPTARG" ;;
    c) SOLR_CORE="$OPTARG" ;;
    t) TIMEOUT="$OPTARG" ;;
    h) usage;;
    \?) usage;;
  esac
done

# Export the URL to be reused in the setup scripts
export DATAVERSE_URL

# Wait for the instance to become available
echo "Waiting for ${DATAVERSE_URL} to become ready in max ${TIMEOUT}."
wait4x http "${DATAVERSE_URL}/api/info/version" -i 8s -t "$TIMEOUT" --expect-status-code 200 --expect-body-json data.version

# Only update an instance that has been bootstrapped before - on a fresh instance, bootstrap.sh
# loads the metadata blocks anyway, so there is nothing to update (and waiting for the
# concurrently running bootstrap here would only reload the same TSV files a second time).
# Same check as in bootstrap.sh.
BLOCK_COUNT=$(curl -sSf "${DATAVERSE_URL}/api/metadatablocks" | jq ".data | length")
if [[ $BLOCK_COUNT -eq 0 ]]; then
  echo "Your instance has not been bootstrapped (yet?), skipping metadata update."
  exit 0
fi

echo "Reloading standard metadata block TSV files..."
"${BOOTSTRAP_DIR}/base/setup-datasetfields.sh"
echo ""

echo "Updating Solr schema for core ${SOLR_CORE} at ${SOLR_URL}..."
solr-driver.sh --mode oneshot --startup-check wait \
  --dataverse-url "${DATAVERSE_URL}" --solr-url "${SOLR_URL}" --core "${SOLR_CORE}"

echo ""
echo "Done. If metadata fields were added or changed, you may want to reindex:"
echo "  curl \"${DATAVERSE_URL}/api/admin/index\" (from within the container network)"
