#!/bin/sh
set -eu

usage() {
    echo "Usage: $0 -d DATABASE_URL -p DATASET_PID" >&2
    exit 1
}

databaseUrl=
datasetPid=

while getopts "d:p:" option; do
    case "$option" in
        d) databaseUrl=$OPTARG ;;
        p) datasetPid=$OPTARG ;;
        *) usage ;;
    esac
done

[ -n "$databaseUrl" ] && [ -n "$datasetPid" ] || usage

scriptDirectory=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
printf 'Capturing query plans for relations of %s\n' "$datasetPid" >&2
psql "$databaseUrl" \
    --pset pager=off \
    -v persistent_id="$datasetPid" \
    -f "$scriptDirectory/explain-list.sql"
