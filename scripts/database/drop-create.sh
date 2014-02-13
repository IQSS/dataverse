#!/bin/bash

# Drops and creates the database. Assumes pg_dump and psql are in $PATH, and that the db does not need password.
DUMP=pg_dump
PSQL=psql
DB=dvndb
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

$DUMP -s $DB > temp-schema.sql
$PSQL -d $DB -f $DIR/drop-all.sql
$PSQL -d $DB -f temp-schema.sql
rm temp-schema.sql