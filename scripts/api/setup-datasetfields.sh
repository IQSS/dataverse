#!/bin/bash

DATAVERSE_URL=${DATAVERSE_URL:-"http://localhost:8080"}
SCRIPT_PATH="$(dirname "$0")"

failed=0

load_metadata() {
    local block="$1"
    shift
    local response http_status body
    local curl_status=0

    response=$(curl -sS -w "\n%{http_code}" "$@") || curl_status=$?
    http_status="${response##*$'\n'}"
    body="${response%$'\n'*}"

    if [[ "$curl_status" -ne 0 || "$http_status" != 2?? ]]; then
        printf 'Failed to load %s (HTTP %s, curl exit %s):\n%s\n' \
            "$block" "$http_status" "$curl_status" "$body" >&2
        failed=1
    else
        printf '%s\n' "$body"
    fi
}

load_metadata "NAControlledVocabularyValue" \
    "${DATAVERSE_URL}/api/admin/datasetfield/loadNAControlledVocabularyValue"

for block in citation geospatial social_science astrophysics biomedical journals 3d_objects; do
    load_metadata "$block" "${DATAVERSE_URL}/api/admin/datasetfield/load" \
        -X POST --data-binary "@${SCRIPT_PATH}/data/metadatablocks/${block}.tsv" \
        -H "Content-type: text/tab-separated-values"
done

exit "$failed"
