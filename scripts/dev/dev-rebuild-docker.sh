#!/bin/sh
echo "Running setup-all.sh (INSECURE MODE)..."
cd scripts/api
./setup-all.sh --insecure -p=admin1 | tee /tmp/setup-all.sh.out
cd ../..

echo "Setting DOI provider to \"FAKE\"..." 
curl http://localhost:8080/api/admin/settings/:DoiProvider -X PUT -d FAKE

export API_TOKEN=`cat /tmp/setup-all.sh.out | grep apiToken| jq .data.apiToken | tr -d \"`

echo "Publishing root dataverse..."
curl -H X-Dataverse-key:$API_TOKEN -X POST http://localhost:8080/api/dataverses/:root/actions/:publish

echo "Allowing users to create dataverses and datasets in root..."
curl -H X-Dataverse-key:$API_TOKEN -X POST -H "Content-type:application/json" -d "{\"assignee\": \":authenticated-users\",\"role\": \"fullContributor\"}" "http://localhost:8080/api/dataverses/:root/assignments"

echo "Checking Dataverse version..."
curl http://localhost:8080/api/info/version
