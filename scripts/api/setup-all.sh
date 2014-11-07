#!/bin/bash
command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }
echo "deleting all data from Solr"
curl http://localhost:8983/solr/update/json?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"

SERVER=http://localhost:8080/api

# Everything + the kitchen sink, in a single script
# - Push the metadata blocks
# - Create the usual suspect users
# - Setup some basic dataverses
# - Index everything

# Setup the metadata blocks, the users, and the dataverses.
TMP=setup.temp

./datasetfields.sh

# Setup the authentication providers
./setup-identity-providers.sh
./setup-users.sh | tee $TMP

PETE=$(cat $TMP | grep :result: | grep Pete | cut -d: -f4)
UMA=$(cat $TMP | grep :result: | grep Uma | cut -d: -f4)

./setup-dvs.sh $PETE $UMA

rm $TMP

# setup the local sign-in
echo Setting up the settings
echo  - Allow internal signup
curl -X PUT "$SERVER/s/settings/:AllowSignUp/yes?key=burrito"
curl -X PUT "$SERVER/s/settings/:SignUpUrl/%2Fdataverseuser.xhtml?key=burrito"
