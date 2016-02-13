#!/bin/bash

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "./bin/dataverse-getopts.sh"

SERVER="http://${OPT_h}:8080/api"

if [ -z ${QUIETMODE+x} ] || [ $QUIETMODE -ne "" ]; then 
  CURL_CMD='curl -s'
  CURL_STDOUT='-o /dev/null'
else
  CURL_CMD='curl'
  CURL_STDOUT=''
fi

# Setup the builtin roles
echo "Setting up admin role"
$CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-admin.json $CURL_STDOUT

echo "Setting up file downloader role"
$CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-filedownloader.json $CURL_STDOUT

echo "Setting up full contributor role"
$CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-fullContributor.json $CURL_STDOUT

echo "Setting up dv contributor role"
$CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-dvContributor.json $CURL_STDOUT

echo "Setting up ds contributor role"
$CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-dsContributor.json $CURL_STDOUT

echo "Setting up editor role"
$CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-editor.json $CURL_STDOUT

echo "Setting up curator role"
$CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-curator.json $CURL_STDOUT

echo "Setting up member role"
$CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-member.json $CURL_STDOUT
