#!/bin/bash

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "./bin/dataverse-getopts.sh"

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
if [[ -e "/dataverse/scripts/api/bin/util-set-verbosity.sh" ]]; then
  . "/dataverse/scripts/api/bin/util-set-verbosity.sh"
elif [[ -e "../../api/bin/util-set-verbosity.sh" ]]; then
  . "../../api/bin/util-set-verbosity.sh"
elif [[ -e "./util-set-verbosity.sh" ]]; then
  . "./util-set-verbosity.sh"
else
  CURL_CMD='curl -s'
  WGET_CMD='wget -q'
  YUM_CMD='yum -q'
  MVN_CMP='mvn -q'
fi

SERVER="http://${OPT_h}:8080/api"

$_IF_INFO echo "Setup the authentication providers"

# Setup the authentication providers
$_IF_VERBOSE echo "Setting up internal user provider"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/authenticationProviders/ -H "Content-type:application/json" -d @data/aupr-builtin.json

$_IF_VERBOSE echo "Setting up Echo providers"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/authenticationProviders/ -H "Content-type:application/json" -d @data/aupr-echo.json
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/authenticationProviders/ -H "Content-type:application/json" -d @data/aupr-echo-dignified.json
