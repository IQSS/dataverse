#!/bin/sh
set -eu

usage() {
    echo "Usage: $0 -d DATABASE_URL -b BASE_URL -k API_TOKEN [-p PARENT_ALIAS] [-n RELATION_COUNT]" >&2
    exit 1
}

databaseUrl=
baseUrl=
apiToken=
parentAlias=root
relationCount=10000
suffix="$(date +%s)$$"
collectionAlias="relationbenchmark${suffix}"
relationTypeName="isSupplementToBenchmark${suffix}"
relationTypeInverseName="isSupplementedByBenchmark${suffix}"

while getopts "d:b:k:p:n:" option; do
    case "$option" in
        d) databaseUrl=$OPTARG ;;
        b) baseUrl=$OPTARG ;;
        k) apiToken=$OPTARG ;;
        p) parentAlias=$OPTARG ;;
        n) relationCount=$OPTARG ;;
        *) usage ;;
    esac
done

[ -n "$databaseUrl" ] && [ -n "$baseUrl" ] && [ -n "$apiToken" ] || usage

baseUrl=${baseUrl%/}

apiPost() {
    description=$1
    endpoint=$2
    body=$3
    responseFile=$(mktemp)
    printf '%s\n' "$description" >&2
    if ! statusCode=$(curl --silent --show-error --output "$responseFile" --write-out '%{http_code}' \
        --header "X-Dataverse-key: $apiToken" \
        --header 'Content-Type: application/json' \
        --data "$body" \
        "$baseUrl$endpoint"); then
        statusCode=000
    fi
    if [ "$statusCode" -lt 200 ] || [ "$statusCode" -ge 300 ]; then
        printf 'Failed while %s (HTTP %s): %s\n' "$description" "$statusCode" "$baseUrl$endpoint" >&2
        cat "$responseFile" >&2
        rm -f "$responseFile"
        return 1
    fi
    cat "$responseFile"
    rm -f "$responseFile"
}

apiDatasetRequest() {
    method=$1
    description=$2
    endpoint=$3
    responseFile=$(mktemp)
    printf '%s\n' "$description" >&2
    if ! statusCode=$(curl --silent --show-error --output "$responseFile" --write-out '%{http_code}' \
        --request "$method" --get \
        --header "X-Dataverse-key: $apiToken" \
        --data-urlencode "persistentId=$targetDatasetPid" \
        "$baseUrl$endpoint"); then
        statusCode=000
    fi
    if [ "$statusCode" -lt 200 ] || [ "$statusCode" -ge 300 ]; then
        printf 'Failed while %s (HTTP %s): %s\n' "$description" "$statusCode" "$baseUrl$endpoint" >&2
        cat "$responseFile" >&2
        rm -f "$responseFile"
        return 1
    fi
    cat "$responseFile"
    rm -f "$responseFile"
}

collectionJson=$(printf '{"alias":"%s","name":"Relation benchmark %s","dataverseType":"UNCATEGORIZED","dataverseContacts":[{"contactEmail":"benchmark@example.org"}]}' "$collectionAlias" "$suffix")
apiPost "Creating temporary collection '$collectionAlias' under '$parentAlias'" "/api/dataverses/$parentAlias" "$collectionJson" >/dev/null
apiPost "Publishing temporary collection '$collectionAlias'" "/api/dataverses/$collectionAlias/actions/:publish" '' >/dev/null

datasetJson='{"datasetVersion":{"license":{"name":"CC0 1.0","uri":"http://creativecommons.org/publicdomain/zero/1.0"},"metadataBlocks":{"citation":{"fields":[{"typeName":"title","multiple":false,"typeClass":"primitive","value":"Dataset relation benchmark"},{"typeName":"author","multiple":true,"typeClass":"compound","value":[{"authorName":{"typeName":"authorName","multiple":false,"typeClass":"primitive","value":"Benchmark, Relation"}}]},{"typeName":"datasetContact","multiple":true,"typeClass":"compound","value":[{"datasetContactName":{"typeName":"datasetContactName","multiple":false,"typeClass":"primitive","value":"Relation Benchmark"},"datasetContactEmail":{"typeName":"datasetContactEmail","multiple":false,"typeClass":"primitive","value":"benchmark@example.org"}}]},{"typeName":"dsDescription","multiple":true,"typeClass":"compound","value":[{"dsDescriptionValue":{"typeName":"dsDescriptionValue","multiple":false,"typeClass":"primitive","value":"Synthetic dataset for relation-listing benchmarks."}}]},{"typeName":"subject","multiple":true,"typeClass":"controlledVocabulary","value":["Other"]}]}}}}'
datasetResponse=$(apiPost "Creating benchmark dataset in '$collectionAlias'" "/api/dataverses/$collectionAlias/datasets" "$datasetJson")
targetDatasetPid=$(printf '%s' "$datasetResponse" | jq -er '.data.persistentId')

apiDatasetRequest POST "Publishing benchmark dataset '$targetDatasetPid'" "/api/datasets/:persistentId/actions/:publish?type=major" >/dev/null

targetDatasetId=
targetVersionId=
attempt=1
while [ "$attempt" -le 60 ]; do
    datasetResponse=$(apiDatasetRequest GET "Checking publication status for '$targetDatasetPid' (attempt $attempt/60)" "/api/datasets/:persistentId")
    versionState=$(printf '%s' "$datasetResponse" | jq -r '.data.latestVersion.versionState')
    if [ "$versionState" = "RELEASED" ]; then
        targetDatasetId=$(printf '%s' "$datasetResponse" | jq -er '.data.id')
        targetVersionId=$(printf '%s' "$datasetResponse" | jq -er '.data.latestVersion.id')
        break
    fi
    sleep 1
    attempt=$((attempt + 1))
done

[ -n "$targetDatasetId" ] && [ -n "$targetVersionId" ] || {
    echo "The benchmark dataset was not published within 60 seconds." >&2
    exit 1
}

relationTypeJson=$(printf '{"name":"%s","displayName":"Relation benchmark %s","inverse":{"name":"%s","displayName":"Inverse relation benchmark %s"}}' "$relationTypeName" "$suffix" "$relationTypeInverseName" "$suffix")
apiPost "Creating relation type '$relationTypeName'" "/api/datasets/relationTypes" "$relationTypeJson" >/dev/null

scriptDirectory=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
printf 'Seeding %s incoming internal relations in PostgreSQL\n' "$relationCount" >&2
psql "$databaseUrl" \
    -v target_dataset_id="$targetDatasetId" \
    -v target_version_id="$targetVersionId" \
    -v relation_count="$relationCount" \
    -v relation_type_name="$relationTypeName" \
    -f "$scriptDirectory/seed-internal-relations.sql"

printf 'Seeded %s internal relations for %s (dataset ID %s, version ID %s).\n' \
    "$relationCount" "$targetDatasetPid" "$targetDatasetId" "$targetVersionId"
printf 'Measure with: %s/measure-list.sh -b %s -p %s \n' \
    "$scriptDirectory" "$baseUrl" "$targetDatasetPid"
