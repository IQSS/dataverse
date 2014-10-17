#!/bin/bash
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
echo "Setting up internal user provider"
curl -H "Content-type:application/json" -d @data/aupr-builtin.json http://localhost:8080/api/s/authenticationProviders/

echo "Setting up Echo providers"
curl -H "Content-type:application/json" -d @data/aupr-echo.json http://localhost:8080/api/s/authenticationProviders/
curl -H "Content-type:application/json" -d @data/aupr-echo-dignified.json http://localhost:8080/api/s/authenticationProviders/

./setup-users.sh | tee $TMP

PETE=$(cat $TMP | grep :result: | grep Pete | cut -d: -f4)
UMA=$(cat $TMP | grep :result: | grep Uma | cut -d: -f4)

./setup-dvs.sh $PETE $UMA

rm $TMP

# setup the local sign-in
echo Setting up the settings
echo  - Allow internal signup
curl -X PUT $SERVER/s/settings/:AllowSignUp/yes
curl -X PUT $SERVER/s/settings/:SignUpUrl/%2Fdataverseuser.xhtml%3FeditMode%3DCREATE
