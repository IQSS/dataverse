#!/bin/bash
command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }

if [[ -n ${1} ]]; then 
  SERVER=http://${1}:8080/api
else
  SERVER=http://localhost:8080/api
fi

if [[ -e /dataverse/scripts/api/data/userPete.json ]]; then
  _dataDir=/dataverse/scripts/api/data
elif [[ -e ../api/data/userPete.json ]]; then
  _dataDir=../api/data
else
  echo "Unable to find data directory with users Pete/Uma/...!" >&2
  echo "Dataverse Test Setup Failed!" >&2
  exit 1;
fi

echo
echo Setting up users on $SERVER
echo ==============================================

curl -s -L -X PUT -d burrito $SERVER/admin/settings/BuiltinUsers.KEY

adminResp=$(curl -s -L -X GET "$SERVER/builtin-users/dataverseAdmin/api-token?password=admin")
echo $adminResp

peteResp=$(curl -s -L -H "Content-type:application/json" -X POST -d @${_dataDir}/userPete.json "$SERVER/builtin-users?password=pete&key=burrito")
echo $peteResp

umaResp=$(curl -s -L -H "Content-type:application/json" -X POST -d @${_dataDir}/userUma.json "$SERVER/builtin-users?password=uma&key=burrito")
echo $umaResp

curl -s -L -H "Content-type:application/json" -X POST -d @${_dataDir}/userGabbi.json "$SERVER/builtin-users?password=gabbi&key=burrito"
echo

curl -s -L -H "Content-type:application/json" -X POST -d @${_dataDir}/userCathy.json "$SERVER/builtin-users?password=cathy&key=burrito"
echo

curl -s -L -H "Content-type:application/json" -X POST -d @${_dataDir}/userNick.json "$SERVER/builtin-users?password=nick&key=burrito"
echo

adminKey=$(echo $adminResp | jq .data.message | tr -d \")
echo :result: dataverseAdmin\'s key is: $adminKey
peteKey=$(echo $peteResp | jq .data.apiToken | tr -d \")
echo :result: Pete\'s key is: $peteKey
umaKey=$(echo $umaResp | jq .data.apiToken | tr -d \")
echo :result: Uma\'s key is: $umaKey
