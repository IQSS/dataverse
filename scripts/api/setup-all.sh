#!/bin/bash
command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }

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

# Everything + the kitchen sink, in a single script
# - Setup the connection with solr / solrCloud
# - Setup the metadata blocks and controlled vocabulary
# - Setup the builtin roles
# - Setup the authentication providers
# - setup the settings (local sign-in)
# - Create admin user and root dataverse
# - (optional) Setup optional users and dataverses

echo "Setup the solr / solrCloud connection"
./setup-solr.sh $@

echo "Setup the metadata blocks"
./setup-datasetfields.sh $@

echo "Setup the builtin roles"
./setup-builtin-roles.sh $@

echo "Setup the authentication providers"
./setup-identity-providers.sh $@

echo "Configuring dataverse server settings"
echo  "- Allow internal signup"
$CURL_CMD "${SERVER}/admin/settings/:AllowSignUp" -X PUT -d yes $CURL_STDOUT
$CURL_CMD "${SERVER}/admin/settings/:SignUpUrl" -X PUT -d /dataverseuser.xhtml?editMode=CREATE $CURL_STDOUT
$CURL_CMD "${SERVER}/admin/settings/:Protocol" -X PUT -d doi $CURL_STDOUT
$CURL_CMD "${SERVER}/admin/settings/:Authority" -X PUT -d 10.5072/FK2 $CURL_STDOUT
$CURL_CMD "${SERVER}/admin/settings/:DoiProvider" -X PUT -d EZID $CURL_STDOUT
$CURL_CMD "${SERVER}/admin/settings/:DoiSeparator" -X PUT -d / $CURL_STDOUT
$CURL_CMD "${SERVER}/admin/settings/BuiltinUsers.KEY" -X PUT -d burrito $CURL_STDOUT
$CURL_CMD "${SERVER}/admin/settings/:BlockedApiKey" -X PUT -d empanada $CURL_STDOUT
$CURL_CMD "${SERVER}/admin/settings/:BlockedApiPolicy" -X PUT -d localhost-only $CURL_STDOUT

echo "Setting up the admin user (and as superuser)"
adminResp=$($CURL_CMD "${SERVER}/builtin-users?password=admin&key=burrito" -s -H "Content-type:application/json" -X POST -d @data/user-admin.json)
#echo $adminResp
$CURL_CMD "${SERVER}/admin/superuser/dataverseAdmin" -X POST $CURL_STDOUT

echo "Setting up the root dataverse"
adminKey=$(echo $adminResp | jq .data.apiToken | tr -d \")
$CURL_CMD "${SERVER}/dataverses/?key=$adminKey" -s -H "Content-type:application/json" -X POST -d @data/dv-root.json $CURL_STDOUT

echo "Set the metadata block for Root"
$CURL_CMD "${SERVER}/dataverses/:root/metadatablocks/?key=$adminKey" -s -X POST -H "Content-type:application/json" -d "[\"citation\"]" $CURL_STDOUT

echo "Set the default facets for Root"
$CURL_CMD "${SERVER}/dataverses/:root/facets/?key=$adminKey" -s -X POST -H "Content-type:application/json" -d "[\"authorName\",\"subject\",\"keywordValue\",\"dateOfDeposit\"]" $CURL_STDOUT

# OPTIONAL USERS AND DATAVERSES
#./setup-optional.sh $@

echo "Setup done. Consider running post-install-api-block.sh for blocking the sensitive API."
