#!/bin/bash

## source-ing setup-getopts.sh provides a common set of shell parameter parsing OPT_* variables for dataverse/environment configuration
. "./setup-getopts.sh"

SERVER="http://${OPT_h}:8080/api"

if [ -z ${QUIETMODE+x} ] || [ $QUIETMODE -ne "" ]; then 
  CURL_CMD='curl -s'
  CURL_STDOUT='-o /dev/null'
else
  CURL_CMD='curl'
  CURL_STDOUT=''
fi

# Setup the authentication providers
echo "Setting up internal user provider"
$CURL_CMD ${SERVER}/admin/authenticationProviders/ -H "Content-type:application/json" -d @data/aupr-builtin.json $CURL_STDOUT

echo "Setting up Echo providers"
$CURL_CMD ${SERVER}/admin/authenticationProviders/ -H "Content-type:application/json" -d @data/aupr-echo.json $CURL_STDOUT
$CURL_CMD ${SERVER}/admin/authenticationProviders/ -H "Content-type:application/json" -d @data/aupr-echo-dignified.json $CURL_STDOUT
