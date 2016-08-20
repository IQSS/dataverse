#!/bin/bash

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "./bin/dataverse-getopts.sh"

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
. "./bin/util-set-verbosity.sh"

SERVER="http://${OPT_h}:8080/api"

#### Setup the builtin roles
$_IF_INFO echo "Setting up the builtin roles ..."

$_IF_VERBOSE echo "Setting up admin role"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-admin.json

$_IF_VERBOSE echo "Setting up file downloader role"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-filedownloader.json

$_IF_VERBOSE echo "Setting up full contributor role"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-fullContributor.json

$_IF_VERBOSE echo "Setting up dv contributor role"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-dvContributor.json

$_IF_VERBOSE echo "Setting up ds contributor role"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-dsContributor.json

$_IF_VERBOSE echo "Setting up editor role"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-editor.json

$_IF_VERBOSE echo "Setting up curator role"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-curator.json

$_IF_VERBOSE echo "Setting up member role"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/roles/ -H "Content-type:application/json" -d @data/role-member.json

$_IF_INFO echo "Builtin Roles setup complete"