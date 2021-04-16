#!/bin/bash
PSQL=psql
DB_NAME=dataverse_app
SQL_FILENAME=dropall.sql

$PSQL $DB_NAME -t -c"SELECT 'drop table \"' || tablename || '\" cascade;' FROM pg_tables WHERE schemaname='public';" > $SQL_FILENAME
$PSQL $DB_NAME -a -f $SQL_FILENAME
rm $SQL_FILENAME
