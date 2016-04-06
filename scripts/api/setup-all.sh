#!/bin/bash
command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY=1; fi
if [[ -z ${SECURESETUP} ]]; then SECURESETUP=1; fi

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
for opt in $*
do
  ## Remove and process unique and/or long command-line options without arguments
  delim=""
  case $opt in
    "--insecure") SECURESETUP=0;;
    "-insecure") SECURESETUP=0;;
    *) [[ "${arg:0:1}" == "-" ]] || delim="\""
      opts="${opts}${delim}${opt}${delim} ";;
  esac
done
eval set -- $opts
. "./bin/dataverse-getopts.sh"

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
. "./bin/util-set-verbosity.sh"

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
if [[ -n $OPT_a ]]; then
  $_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:SystemEmail" -X PUT -d "${OPT_a}"
fi
if [[ -n $OPT_t ]]; then
  $_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:TwoRavensUrl" -X PUT -d "${OPT_t}"
  $_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:TwoRavensTabularView" -X PUT -d true
fi
$_IF_VERBOSE echo  "- Allow internal signup"
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:AllowSignUp" -X PUT -d yes
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:SignUpUrl" -X PUT -d /dataverseuser.xhtml?editMode=CREATE
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:Protocol" -X PUT -d doi
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:Authority" -X PUT -d 10.5072/FK2
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:DoiProvider" -X PUT -d EZID
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/:DoiSeparator" -X PUT -d /
$_IF_VERBOSE $CURL_CMD "${SERVER}/admin/settings/BuiltinUsers.KEY" -X PUT -d burrito
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

if [ $SECURESETUP = 1 ]
then
    # Revoke the "burrito" super-key; 
    # Block the sensitive API endpoints;
    $_IF_VERBOSE $CURL_CMD -X DELETE $SERVER/admin/settings/BuiltinUsers.KEY
    $_IF_VERBOSE $CURL_CMD -X PUT -d admin,test $SERVER/admin/settings/:BlockedApiEndpoints
    $_IF_TERSE echo "Access to the /api/admin and /api/test is now disabled, except for connections from localhost."
else 
    $_IF_INFO echo "IMPORTANT!!!"
    $_IF_INFO echo "You have run the setup script in the INSECURE mode!"
    $_IF_INFO echo "Do keep in mind, that access to your admin API is now WIDE-OPEN!"
    $_IF_INFO echo "Also, your built-in user is still set up with the default authentication token"
    $_IF_INFO echo "(that is distributed as part of this script, hence EVERYBODY KNOWS WHAT IT IS!)"
    $_IF_INFO echo "Please consider the consequences of this choice. You can block access to the"
    $_IF_INFO echo "/api/admin and /api/test endpoints, for all connections except from localhost,"
    $_IF_INFO echo "and revoke the authentication token from the built-in user by executing the"
    $_IF_INFO echo "script post-install-api-block.sh."
fi

$_IF_TERSE echo "Setup done."
