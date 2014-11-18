#!/bin/bash
command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }

# OPTIONAL USERS AND DATAVERSES
TMP=setup.temp
./setup-users.sh | tee $TMP

PETE=$(cat $TMP | grep :result: | grep Pete | cut -d: -f4)
UMA=$(cat $TMP | grep :result: | grep Uma | cut -d: -f4)

./setup-dvs.sh $PETE $UMA

rm $TMP
