#!/bin/sh
set -eu

usage() {
    echo "Usage: $0 -d DATABASE_URL" >&2
    exit 1
}

databaseUrl=

while getopts "d:" option; do
    case "$option" in
        d) databaseUrl=$OPTARG ;;
        *) usage ;;
    esac
done

[ -n "$databaseUrl" ] || usage

scriptDirectory=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
printf 'Cleaning up benchmark data in PostgreSQL\n' >&2
psql "$databaseUrl" \
    -f "$scriptDirectory/cleanup.sql"
