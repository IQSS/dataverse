#!/bin/bash

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "./bin/dataverse-getopts.sh"

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
. "./bin/util-set-verbosity.sh"

SERVER="http://${OPT_h}:8080/api"

$_IF_INFO echo "Setup the authentication providers"

# Setup the authentication providers
$_IF_VERBOSE echo "Setting up internal user provider"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/authenticationProviders/ -H "Content-type:application/json" -d @data/aupr-builtin.json

$_IF_VERBOSE echo "Setting up Echo providers"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/authenticationProviders/ -H "Content-type:application/json" -d @data/aupr-echo.json
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/authenticationProviders/ -H "Content-type:application/json" -d @data/aupr-echo-dignified.json
