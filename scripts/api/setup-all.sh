#!/bin/bash

SECURESETUP=1
DV_SU_PASSWORD="admin"

DATAVERSE_URL=${DATAVERSE_URL:-"http://localhost:8080"}
# Make sure scripts we call from this one also get this env var!
export DATAVERSE_URL

# scripts/api when called from the root of the source tree
SCRIPT_PATH="$(dirname "$0")"

for opt in "$@"
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

# shellcheck disable=SC2016
command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }

# Everything + the kitchen sink, in a single script
# - Setup the metadata blocks and controlled vocabulary
# - Setup the builtin roles
# - Setup the authentication providers
# - setup the settings (local sign-in)
# - Create admin user and root dataverse
# - (optional) Setup optional users and dataverses


echo "Setup the metadata blocks"
"$SCRIPT_PATH"/setup-datasetfields.sh

echo "Setup the builtin roles"
"$SCRIPT_PATH"/setup-builtin-roles.sh

echo "Setup the authentication providers"
"$SCRIPT_PATH"/setup-identity-providers.sh

echo "Setting up the settings"
echo  "- Allow internal signup"
curl -X PUT -d yes "${DATAVERSE_URL}/api/admin/settings/:AllowSignUp"
curl -X PUT -d "/dataverseuser.xhtml?editMode=CREATE" "${DATAVERSE_URL}/api/admin/settings/:SignUpUrl"

curl -X PUT -d burrito "${DATAVERSE_URL}/api/admin/settings/BuiltinUsers.KEY"
curl -X PUT -d localhost-only "${DATAVERSE_URL}/api/admin/settings/:BlockedApiPolicy"
curl -X PUT -d 'native/http' "${DATAVERSE_URL}/api/admin/settings/:UploadMethods"
echo

echo "Setting up the admin user (and as superuser)"
adminResp=$(curl -s -H "Content-type:application/json" -X POST -d @"$SCRIPT_PATH"/data/user-admin.json "${DATAVERSE_URL}/api/builtin-users?password=$DV_SU_PASSWORD&key=burrito")
echo "$adminResp"
curl -X POST "${DATAVERSE_URL}/api/admin/superuser/dataverseAdmin"
echo

echo "Setting up the root dataverse"
adminKey=$(echo "$adminResp" | jq .data.apiToken | tr -d \")
curl -s -H "Content-type:application/json" -X POST -d @"$SCRIPT_PATH"/data/dv-root.json "${DATAVERSE_URL}/api/dataverses/?key=$adminKey"
echo
echo "Set the metadata block for Root"
curl -s -X POST -H "Content-type:application/json" -d "[\"citation\"]" "${DATAVERSE_URL}/api/dataverses/:root/metadatablocks/?key=$adminKey"
echo
echo "Set the default facets for Root"
curl -s -X POST -H "Content-type:application/json" -d "[\"authorName\",\"subject\",\"keywordValue\",\"dateOfDeposit\"]" "${DATAVERSE_URL}/api/dataverses/:root/facets/?key=$adminKey"
echo

echo "Set up licenses"
# Note: CC0 has been added and set as the default license through
# Flyway script V5.9.0.1__7440-configurable-license-list.sql
curl -X POST -H 'Content-Type: application/json' -H "X-Dataverse-key:$adminKey" "${DATAVERSE_URL}/api/licenses" --upload-file "$SCRIPT_PATH"/data/licenses/licenseCC-BY-4.0.json

# OPTIONAL USERS AND DATAVERSES
#./setup-optional.sh

if [ $SECURESETUP = 1 ]
then
    # Revoke the "burrito" super-key; 
    # Block sensitive API endpoints;
    curl -X DELETE "${DATAVERSE_URL}/api/admin/settings/BuiltinUsers.KEY"
    curl -X PUT -d 'admin,builtin-users' "${DATAVERSE_URL}/api/admin/settings/:BlockedApiEndpoints"
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
