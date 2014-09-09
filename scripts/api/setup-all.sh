#!/bin/bash

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

# index-all
# FIXME: obviate the need for this
curl -s http://localhost:8080/api/index
