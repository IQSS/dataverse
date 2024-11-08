#!/bin/bash

set -euo pipefail

# Set some defaults as documented
DATAVERSE_URL=${DATAVERSE_URL:-"http://dataverse:8080"}
export DATAVERSE_URL

echo "Running base setup-all.sh (INSECURE MODE)..."
"${BOOTSTRAP_DIR}"/base/setup-all.sh --insecure -p=admin1 | tee /tmp/setup-all.sh.out

echo "Setting DOI provider to \"FAKE\"..."
curl "${DATAVERSE_URL}/api/admin/settings/:DoiProvider" -X PUT -d FAKE

API_TOKEN=$(grep apiToken "/tmp/setup-all.sh.out" | jq ".data.apiToken" | tr -d \")
export API_TOKEN
# ${ENV_OUT} comes from bootstrap.sh and will expose the saved information back to the host if enabled.
echo "API_TOKEN=${API_TOKEN}" >> "${ENV_OUT}"

echo "Loading CodeMeta metadata block (needed for API tests)..."
curl "${DATAVERSE_URL}/api/admin/datasetfield/load" -X POST --data-binary @/scripts/bootstrap/base/data/metadatablocks/codemeta.tsv -H "Content-type: text/tab-separated-values"

echo "Fetching Solr schema from Dataverse and running update-fields.sh..."
curl "${DATAVERSE_URL}/api/admin/index/solr/schema" | /scripts/update-fields.sh /var/solr/data/collection1/conf/schema.xml

echo "Reloading Solr..."
curl "http://solr:8983/solr/admin/cores?action=RELOAD&core=collection1"

echo "Publishing root dataverse..."
curl -H "X-Dataverse-key:$API_TOKEN" -X POST "${DATAVERSE_URL}/api/dataverses/:root/actions/:publish"

echo "Allowing users to create dataverses and datasets in root..."
curl -H "X-Dataverse-key:$API_TOKEN" -X POST -H "Content-type:application/json" -d "{\"assignee\": \":authenticated-users\",\"role\": \"fullContributor\"}" "${DATAVERSE_URL}/api/dataverses/:root/assignments"

echo "Checking Dataverse version..."
curl "${DATAVERSE_URL}/api/info/version"

echo ""
echo "Done, your instance has been configured for development. Have a nice day!"
