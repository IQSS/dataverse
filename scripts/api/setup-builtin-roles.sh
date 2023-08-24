#!/bin/bash

DATAVERSE_URL=${DATAVERSE_URL:-"http://localhost:8080"}
SCRIPT_PATH="$(dirname "$0")"

# Setup the builtin roles
echo "Setting up admin role"
curl -H "Content-type:application/json" -d @"$SCRIPT_PATH"/data/role-admin.json "${DATAVERSE_URL}/api/admin/roles/"
echo

echo "Setting up file downloader role"
curl -H "Content-type:application/json" -d @"$SCRIPT_PATH"/data/role-filedownloader.json "${DATAVERSE_URL}/api/admin/roles/"
echo

echo "Setting up full contributor role"
curl -H "Content-type:application/json" -d @"$SCRIPT_PATH"/data/role-fullContributor.json "${DATAVERSE_URL}/api/admin/roles/"
echo

echo "Setting up dv contributor role"
curl -H "Content-type:application/json" -d @"$SCRIPT_PATH"/data/role-dvContributor.json "${DATAVERSE_URL}/api/admin/roles/"
echo

echo "Setting up ds contributor role"
curl -H "Content-type:application/json" -d @"$SCRIPT_PATH"/data/role-dsContributor.json "${DATAVERSE_URL}/api/admin/roles/"
echo

echo "Setting up editor role"
curl -H "Content-type:application/json" -d @"$SCRIPT_PATH"/data/role-editor.json "${DATAVERSE_URL}/api/admin/roles/"
echo

echo "Setting up curator role"
curl -H "Content-type:application/json" -d @"$SCRIPT_PATH"/data/role-curator.json "${DATAVERSE_URL}/api/admin/roles/"
echo

echo "Setting up member role"
curl -H "Content-type:application/json" -d @"$SCRIPT_PATH"/data/role-member.json "${DATAVERSE_URL}/api/admin/roles/"
echo
