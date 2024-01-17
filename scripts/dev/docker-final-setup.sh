#!/bin/sh

set -euo pipefail

echo "Running setup-all.sh (INSECURE MODE)..."
cd scripts/api || exit
./setup-all.sh --insecure -p=admin1 | tee /tmp/setup-all.sh.out
cd ../..

echo "Setting system mail address..."
curl -X PUT -d "dataverse@localhost" "http://localhost:8080/api/admin/settings/:SystemEmail"

echo "Setting DOI provider to \"FAKE\"..."
curl "http://localhost:8080/api/admin/settings/:DoiProvider" -X PUT -d FAKE

API_TOKEN=$(grep apiToken "/tmp/setup-all.sh.out" | jq ".data.apiToken" | tr -d \")
export API_TOKEN

echo "Publishing root dataverse..."
curl -H "X-Dataverse-key:$API_TOKEN" -X POST "http://localhost:8080/api/dataverses/:root/actions/:publish"

echo "Allowing users to create dataverses and datasets in root..."
curl -H "X-Dataverse-key:$API_TOKEN" -X POST -H "Content-type:application/json" -d "{\"assignee\": \":authenticated-users\",\"role\": \"fullContributor\"}" "http://localhost:8080/api/dataverses/:root/assignments"

echo "Checking Dataverse version..."
curl "http://localhost:8080/api/info/version"