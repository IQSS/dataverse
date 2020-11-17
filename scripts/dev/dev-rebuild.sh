#!/bin/sh
PAYARA_DIR=/usr/local/payara5
ASADMIN=$PAYARA_DIR/glassfish/bin/asadmin
DB_NAME=dvndb
DB_USER=dvnapp

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
psql -U postgres -c "
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = '$DB_NAME'
  AND pid <> pg_backend_pid();
" template1

echo "Dropping the database..."
psql -U $DB_USER -c "DROP DATABASE \"$DB_NAME\"" template1
echo $?

echo "Clearing out data from Solr..."
curl http://localhost:8983/solr/collection1/update/json?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"

echo "Creating a new database..."
psql -U $DB_USER -c "CREATE DATABASE \"$DB_NAME\" WITH OWNER = \"$DB_USER\"" template1
echo $?

echo "Starting app server..."
$PAYARA_DIR/glassfish/bin/asadmin start-domain

echo "Deploying war file..."
$PAYARA_DIR/glassfish/bin/asadmin deploy $NEW_WAR

echo "Running setup-all.sh (INSECURE MODE)..."
cd scripts/api
./setup-all.sh --insecure -p=admin1 | tee /tmp/setup-all.sh.out
cd ../..

echo "Loading SQL reference data..."
psql -U $DB_USER $DB_NAME -f scripts/database/reference_data.sql

echo "Creating SQL sequence..."
psql -U $DB_USER $DB_NAME -f doc/sphinx-guides/source/_static/util/createsequence.sql

echo "Setting DOI provider to \"FAKE\"..." 
curl http://localhost:8080/api/admin/settings/:DoiProvider -X PUT -d FAKE
export API_TOKEN=`cat /tmp/setup-all.sh.out | grep apiToken| jq .data.apiToken | tr -d \"`

echo "Publishing root dataverse..."
curl -H X-Dataverse-key:$API_TOKEN -X POST http://localhost:8080/api/dataverses/:root/actions/:publish

echo "Allowing users to create dataverses and datasets in root..."
curl -H X-Dataverse-key:$API_TOKEN -X POST -H "Content-type:application/json" -d "{\"assignee\": \":authenticated-users\",\"role\": \"fullContributor\"}" "http://localhost:8080/api/dataverses/:root/assignments"

echo "Checking Dataverse version..."
curl http://localhost:8080/api/info/version
