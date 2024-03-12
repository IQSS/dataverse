#!/bin/sh
PAYARA_DIR=/usr/local/payara6
ASADMIN=$PAYARA_DIR/glassfish/bin/asadmin
DB_NAME=dvndb
DB_USER=dvnapp
export PGPASSWORD=secret

echo "Checking if there is a war file to undeploy..."
LIST_APP=$($ASADMIN list-applications -t)
OLD_WAR=$(echo $LIST_APP | awk '{print $1}')
NEW_WAR=target/dataverse*.war
if [ ! -z $OLD_WAR ]; then
  $ASADMIN undeploy $OLD_WAR
fi

echo "Stopping app server..."
$ASADMIN stop-domain

echo "Deleting \"generated\" directory..."
rm -rf $PAYARA_DIR/glassfish/domains/domain1/generated

echo "Deleting ALL DATA FILES uploaded to Dataverse..." 
# TODO: Make this configurable.
rm -rf $PAYARA_DIR/glassfish/domains/domain1/files

echo "Terminating database sessions so we can drop the database..."
psql -h localhost -U postgres -c "
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = '$DB_NAME'
  AND pid <> pg_backend_pid();
" template1

echo "Dropping the database..."
psql -h localhost -U $DB_USER -c "DROP DATABASE \"$DB_NAME\"" template1
echo $?

echo "Clearing out data from Solr..."
curl "http://localhost:8983/solr/collection1/update/json?commit=true" -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"

echo "Creating a new database..."
psql -h localhost -U $DB_USER -c "CREATE DATABASE \"$DB_NAME\" WITH OWNER = \"$DB_USER\"" template1
echo $?

echo "Starting app server..."
$PAYARA_DIR/glassfish/bin/asadmin start-domain

echo "Deploying war file..."
$PAYARA_DIR/glassfish/bin/asadmin deploy $NEW_WAR

echo "Running setup-all.sh (INSECURE MODE)..."
cd scripts/api
./setup-all.sh --insecure -p=admin1 | tee /tmp/setup-all.sh.out
cd ../..

echo "Creating SQL sequence..."
psql -h localhost -U $DB_USER $DB_NAME -f doc/sphinx-guides/source/_static/util/createsequence.sql

echo "Allowing GUI edits to be visible without redeploy..."
$PAYARA_DIR/glassfish/bin/asadmin create-system-properties "dataverse.jsf.refresh-period=1"

export API_TOKEN=`cat /tmp/setup-all.sh.out | grep apiToken| jq .data.apiToken | tr -d \"`

echo "Publishing root dataverse..."
curl -H X-Dataverse-key:$API_TOKEN -X POST http://localhost:8080/api/dataverses/:root/actions/:publish

echo "Allowing users to create dataverses and datasets in root..."
curl -H X-Dataverse-key:$API_TOKEN -X POST -H "Content-type:application/json" -d "{\"assignee\": \":authenticated-users\",\"role\": \"fullContributor\"}" "http://localhost:8080/api/dataverses/:root/assignments"

echo "Checking Dataverse version..."
curl http://localhost:8080/api/info/version
