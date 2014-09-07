#!/bin/bash

# Setup the metadata blocks, the users, and the dataverses.
TMP=setup.temp

./datasetfields.sh

./setup-users.sh | tee $TMP

PETE=$(cat $TMP | grep :result: | grep Pete | cut -d: -f4)
UMA=$(cat $TMP | grep :result: | grep Uma | cut -d: -f4)

./setup-dvs.sh $PETE $UMA

rm $TMP