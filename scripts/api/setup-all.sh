#!/bin/bash

SECURESETUP=1
DV_SU_PASSWORD="admin"

for opt in $*
do
  case $opt in
      "--insecure")
	  SECURESETUP=0
	  ;;
      "-insecure")
	  SECURESETUP=0;
	  ;;
      -p=*)
	  # https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash/14203146#14203146
	  DV_SU_PASSWORD="${opt#*=}"
	  shift # past argument=value
	  ;;
      *)
	  echo "invalid option: $opt"
	  exit 1 >&2
	  ;;
  esac
done

command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }

echo "deleting all data from Solr"
curl http://localhost:8983/solr/collection1/update/json?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"

SERVER=http://localhost:8080/api

# Everything + the kitchen sink, in a single script
# - Setup the metadata blocks and controlled vocabulary
# - Setup the builtin roles
# - Setup the authentication providers
# - setup the settings (local sign-in)
# - Create admin user and root dataverse
# - (optional) Setup optional users and dataverses


echo "Setup the metadata blocks"
./setup-datasetfields.sh

echo "Setup the builtin roles"
./setup-builtin-roles.sh

echo "Setup the authentication providers"
./setup-identity-providers.sh

echo "Setting up the settings"
echo  "- Allow internal signup"
curl -X PUT -d yes "$SERVER/admin/settings/:AllowSignUp"
curl -X PUT -d /dataverseuser.xhtml?editMode=CREATE "$SERVER/admin/settings/:SignUpUrl"

curl -X PUT -d doi "$SERVER/admin/settings/:Protocol"
curl -X PUT -d 10.5072 "$SERVER/admin/settings/:Authority"
curl -X PUT -d "FK2/" "$SERVER/admin/settings/:Shoulder"
curl -X PUT -d EZID "$SERVER/admin/settings/:DoiProvider"
curl -X PUT -d burrito $SERVER/admin/settings/BuiltinUsers.KEY
curl -X PUT -d localhost-only $SERVER/admin/settings/:BlockedApiPolicy
echo

echo "Setting up the admin user (and as superuser)"
adminResp=$(curl -s -H "Content-type:application/json" -X POST -d @data/user-admin.json "$SERVER/builtin-users?password=$DV_SU_PASSWORD&key=burrito")
echo $adminResp
curl -X POST "$SERVER/admin/superuser/dataverseAdmin"
echo

echo "Setting up the root dataverse"
adminKey=$(echo $adminResp | jq .data.apiToken | tr -d \")
curl -s -H "Content-type:application/json" -X POST -d @data/dv-root.json "$SERVER/dataverses/?key=$adminKey"
echo
echo "Set the metadata block for Root"
curl -s -X POST -H "Content-type:application/json" -d "[\"citation\"]" $SERVER/dataverses/:root/metadatablocks/?key=$adminKey
echo
echo "Set the default facets for Root"
curl -s -X POST -H "Content-type:application/json" -d "[\"authorName\",\"subject\",\"keywordValue\",\"dateOfDeposit\"]" $SERVER/dataverses/:root/facets/?key=$adminKey
echo

# OPTIONAL USERS AND DATAVERSES
#./setup-optional.sh

if [ $SECURESETUP = 1 ]
then
    # Revoke the "burrito" super-key; 
    # Block the sensitive API endpoints;
    curl -X DELETE $SERVER/admin/settings/BuiltinUsers.KEY
    curl -X PUT -d admin,test $SERVER/admin/settings/:BlockedApiEndpoints
    echo "Access to the /api/admin and /api/test is now disabled, except for connections from localhost."
else 
    echo "IMPORTANT!!!"
    echo "You have run the setup script in the INSECURE mode!"
    echo "Do keep in mind, that access to your admin API is now WIDE-OPEN!"
    echo "Also, your built-in user is still set up with the default authentication token"
    echo "(that is distributed as part of this script, hence EVERYBODY KNOWS WHAT IT IS!)"
    echo "Please consider the consequences of this choice. You can block access to the"
    echo "/api/admin and /api/test endpoints, for all connections except from localhost,"
    echo "and revoke the authentication token from the built-in user by executing the"
    echo "script post-install-api-block.sh."
fi

echo
echo "Setup done."
