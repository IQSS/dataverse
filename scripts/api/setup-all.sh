#!/bin/bash
command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }

echo "deleting all data from Solr"
curl http://localhost:8983/solr/update/json?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"

SERVER=http://localhost:8080/api

# Everything + the kitchen sink, in a single script
# - Setup the metadata blocks and controlled vocabulary
# - Setup the builtin roles
# - Setup the authentication providers
# - setup the settings (local sign-in)
# - Create admin user and root dataverse
# - (optional) Setup optional users and dataverses


echo "Setup the metadata blocks"
./setup-datasetfields.sh

echo "Setup the builtin roles"
./setup-builtin-roles.sh

echo "Setup the authentication providers"
./setup-identity-providers.sh

echo "Setting up the settings"
echo  "- Allow internal signup"
curl -X PUT -d yes "$SERVER/s/settings/:AllowSignUp"
curl -X PUT -d /dataverseuser.xhtml?editMode=CREATE "$SERVER/s/settings/:SignUpUrl"

curl -X PUT -d doi "$SERVER/s/settings/:Protocol"
curl -X PUT -d 10.5072/FK2 "$SERVER/s/settings/:Authority"
curl -X PUT -d EZID "$SERVER/s/settings/:DoiProvider"
curl -X PUT -d / "$SERVER/s/settings/:DoiSeparator"
curl -X PUT -d burrito $SERVER/s/settings/BuiltinUsers.KEY
echo

echo "Setting up the admin user (and as superuser)"
adminResp=$(curl -s -H "Content-type:application/json" -X POST -d @data/user-admin.json "$SERVER/users?password=admin&key=burrito")
echo $adminResp
curl -X POST "$SERVER/s/superuser/admin"
echo

echo "Setting up the root dataverse"
adminKey=$(echo $adminResp | jq .data.apiToken | tr -d \")
curl -s -H "Content-type:application/json" -X POST -d @data/dv-root.json "$SERVER/dvs/?key=$adminKey"
echo
echo "Set the metadata block for Root"
curl -s -X POST -H "Content-type:application/json" -d "[\"citation\"]" $SERVER/dvs/:root/metadatablocks/?key=$adminKey
echo

echo "Setting up a sample Shibboleth institutional group"
curl -s -X POST -H 'Content-type:application/json' --upload-file data/shibGroupTestShib.json "$SERVER/groups/shib?key=$adminKey" 
echo

# OPTIONAL USERS AND DATAVERSES
#./setup-optional.sh
