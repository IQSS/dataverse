#!/bin/bash

DATAVERSE_URL=${DATAVERSE_URL:-"http://localhost:8080"}
SCRIPT_PATH="$(dirname "$0")"

# Setup the authentication providers
echo "Setting up internal user provider"
curl -H "Content-type:application/json" -d @"$SCRIPT_PATH"/data/authentication-providers/builtin.json "${DATAVERSE_URL}/api/admin/authenticationProviders/"

#echo "Setting up Echo providers"
#curl -H "Content-type:application/json" -d @data/authentication-providers/echo.json http://localhost:8080/api/admin/authenticationProviders/
#curl -H "Content-type:application/json" -d @data/authentication-providers/echo-dignified.json http://localhost:8080/api/admin/authenticationProviders/
