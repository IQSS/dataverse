#!/bin/bash
command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }

if [[ -n ${1} ]]; then 
  SERVER=http://${1}:8080/api
else
  SERVER=http://localhost:8080/api
fi

adminKey=$(curl -s -L -X GET "$SERVER/builtin-users/dataverseAdmin/api-token?password=admin" | jq .data.message | tr -d \")

echo Publishing 'curator-able' root dataverse on $SERVER
echo ====================================================================

curl -s -L -X POST -H "X-Dataverse-key:$adminKey" -H "Content-type:application/json"  -d "{\"assignee\":\":authenticated-users\",\"role\":\"dvContributor\"}" ${SERVER}/dataverses/root/assignments
for i in {1..3}; do
  result=$(curl -s -L -X POST -H "X-Dataverse-key:$adminKey" ${SERVER}/dataverses/root/actions/:publish | jq .status | tr -d \")
  if [[ $result == "OK" ]]; then
    break;
  else
    echo "Failed to publish root dataverse. (Attempt ${i})"
  fi
done