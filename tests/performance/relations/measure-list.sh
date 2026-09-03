#!/bin/sh
set -eu

usage() {
    echo "Usage: $0 -b BASE_URL -p DATASET_PID [-l LIMIT] [-m INCLUDE_METADATA_BLOCKS] [-r REQUESTS] [-w WARMUPS]" >&2
    exit 1
}

baseUrl=
datasetPid=
requests=20
warmups=5
limit=10
includeMetadataBlocks=false
connectTimeout=10
requestTimeout=120

while getopts "b:p:l:m:r:w:" option; do
    case "$option" in
        b) baseUrl=$OPTARG ;;
        p) datasetPid=$OPTARG ;;
        l) limit=$OPTARG ;;
        m) includeMetadataBlocks=$OPTARG ;;
        r) requests=$OPTARG ;;
        w) warmups=$OPTARG ;;
        *) usage ;;
    esac
done

[ -n "$baseUrl" ] && [ -n "$datasetPid" ] || usage

resultsFile=$(mktemp)
trap 'rm -f "$resultsFile"' EXIT

request() {
    responseFile=$(mktemp)
    if ! response=$(curl --silent --show-error --output "$responseFile" --write-out '%{http_code} %{time_total}' \
        --connect-timeout "$connectTimeout" --max-time "$requestTimeout" \
        --get --data-urlencode "persistentId=$datasetPid" --data "limit=$limit" \
        --data "includeMetadataBlocks=$includeMetadataBlocks" \
        "$baseUrl/api/datasets/:persistentId/relations"); then
        response='000 0'
    fi
    statusCode=${response%% *}
    elapsed=${response#* }
    [ "$statusCode" = "200" ] || {
        echo "Request failed with HTTP $statusCode after ${elapsed}s" >&2
        cat "$responseFile" >&2
        rm -f "$responseFile"
        exit 1
    }
    rm -f "$responseFile"
    echo "$elapsed"
}

warmup=1
while [ "$warmup" -le "$warmups" ]; do
    printf 'Warm-up request %s/%s\n' "$warmup" "$warmups" >&2
    elapsed=$(request)
    printf 'Warm-up request %s/%s completed in %ss\n' "$warmup" "$warmups" "$elapsed" >&2
    warmup=$((warmup + 1))
done

requestNumber=1
while [ "$requestNumber" -le "$requests" ]; do
    printf 'Measured request %s/%s\n' "$requestNumber" "$requests" >&2
    elapsed=$(request)
    printf '%s\n' "$elapsed" >>"$resultsFile"
    printf 'Measured request %s/%s completed in %ss\n' "$requestNumber" "$requests" "$elapsed" >&2
    requestNumber=$((requestNumber + 1))
done

sort -n "$resultsFile" | awk '
    { values[NR] = $1; total += $1 }
    END {
        p50 = values[int((NR + 1) * 0.50)]
        p95 = values[int(NR * 0.95 + 0.999999)]
        printf "requests=%d min=%.3fs median=%.3fs p95=%.3fs max=%.3fs mean=%.3fs\n", NR, values[1], p50, p95, values[NR], total / NR
    }
'
