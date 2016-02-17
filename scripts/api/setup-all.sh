#!/bin/bash
command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }

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

$_IF_TERSE echo "Setting up dataverse server http://${OPT_h}:8080"

# Everything + the kitchen sink, in a single script
# - Setup the connection with solr / solrCloud
# - Setup the metadata blocks and controlled vocabulary
# - Setup the builtin roles
# - Setup the authentication providers
# - setup the settings (local sign-in)
# - Create admin user and root dataverse
# - (optional) Setup optional users and dataverses

#### Setup the solr / solrCloud connection
./setup-solr.sh $@

#### Setup the metadata blocks
./setup-datasetfields.sh $@

#### Setup the builtin roles
./setup-builtin-roles.sh $@

#### Setup the authentication providers
./setup-identity-providers.sh $@

$_IF_INFO echo "Configuring dataverse server settings"
$_IF_VERBOSE echo  "- Allow internal signup"
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:AllowSignUp" -X PUT -d yes
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:SignUpUrl" -X PUT -d /dataverseuser.xhtml?editMode=CREATE
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:Protocol" -X PUT -d doi
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:Authority" -X PUT -d 10.5072/FK2
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:DoiProvider" -X PUT -d EZID
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:DoiSeparator" -X PUT -d /
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/BuiltinUsers.KEY" -X PUT -d burrito
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:BlockedApiKey" -X PUT -d empanada
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:BlockedApiPolicy" -X PUT -d localhost-only

$_IF_INFO echo "Setting up the admin user (and as superuser)"
adminResp=$(curl "${SERVER}/builtin-users?password=admin&key=burrito" -s -H "Content-type:application/json" -X POST -d @data/user-admin.json)
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/superuser/dataverseAdmin" -X POST

$_IF_INFO echo "Setting up the root dataverse"
adminKey=$(echo $adminResp | jq .data.apiToken | tr -d \")
$_IF_VERBOSE $CURL_CMD "${SERVER}/dataverses/?key=$adminKey" -s -H "Content-type:application/json" -X POST -d @data/dv-root.json

$_IF_INFO echo "Set the metadata block for Root"
$_IF_VERBOSE $CURL_CMD "${SERVER}/dataverses/:root/metadatablocks/?key=$adminKey" -s -X POST -H "Content-type:application/json" -d "[\"citation\"]"

$_IF_INFO echo "Set the default facets for Root"
$_IF_VERBOSE $CURL_CMD "${SERVER}/dataverses/:root/facets/?key=$adminKey" -s -X POST -H "Content-type:application/json" -d "[\"authorName\",\"subject\",\"keywordValue\",\"dateOfDeposit\"]"

# OPTIONAL USERS AND DATAVERSES
#./setup-optional.sh $@

$_IF_TERSE echo "Setup done. Consider running post-install-api-block.sh for blocking the sensitive API."
