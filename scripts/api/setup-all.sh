#!/bin/bash

SERVER=http://localhost:8080/api

# Everything + the kitchen sink, in a single script
# - Push the metadata blocks
# - Create the usual suspect users
# - Setup some basic dataverses
# - Index everything

# Setup the metadata blocks, the users, and the dataverses.
TMP=setup.temp

./datasetfields.sh

./setup-users.sh | tee $TMP

PETE=$(cat $TMP | grep :result: | grep Pete | cut -d: -f4)
UMA=$(cat $TMP | grep :result: | grep Uma | cut -d: -f4)

./setup-dvs.sh $PETE $UMA

rm $TMP

# setup the local sign-in
echo Setting up the settings
echo  - Allow internal signup
curl -X PUT $SERVER/s/settings/:AllowSignUp/yes
curl -X PUT $SERVER/s/settings/:SignUpUrl/builtin-idp%2Fsignup.xhtml

# index-all
# FIXME: obviate the need for this

echo Start indexing
curl -s http://localhost:8080/api/index
