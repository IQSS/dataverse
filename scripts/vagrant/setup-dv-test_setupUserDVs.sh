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

peteKey=$(curl -s -L -X GET "$SERVER/builtin-users/pete/api-token?password=pete" | jq .data.message | tr -d \")
umaKey=$(curl -s -L -X GET "$SERVER/builtin-users/uma/api-token?password=uma" | jq .data.message | tr -d \")

echo
echo Setting up dataverses on $SERVER
echo ==============================================

echo Pete
for i in {1..3}; do
  result=$(curl -s -L -H "Content-type:application/json" -X POST -d @${_dataDir}/dv-pete-top.json "$SERVER/dataverses/root?key=$peteKey" | jq .status | tr -d \")
  if [[ $result == "OK" ]]; then
    break;
  else
    echo "Failed to create dataverse. (Attempt ${i})"
  fi
done
echo
curl -s -L -H "Content-type:application/json" -X POST -d @${_dataDir}/dv-pete-sub-normal.json "$SERVER/dataverses/peteTop?key=$peteKey"
echo
curl -s -L -H "Content-type:application/json" -X POST -d @${_dataDir}/dv-pete-sub-restricted.json "$SERVER/dataverses/peteTop?key=$peteKey"
echo
curl -s -L -H "Content-type:application/json" -X POST -d @${_dataDir}/dv-pete-sub-secret.json "$SERVER/dataverses/peteTop?key=$peteKey"
echo

echo Uma
echo Pete creates top-level for Uma
curl -s -L -H "Content-type:application/json" -H "X-Dataverse-key:$peteKey" -X POST -d @${_dataDir}/dv-uma-top.json "$SERVER/dataverses/root"
echo
echo Pete makes Uma an admin on her own DV
curl -s -L -H "Content-type:application/json" -H "X-Dataverse-key:$peteKey" -X POST -d"{\"assignee\":\"@uma\",\"role\":\"admin\"}" $SERVER/dataverses/umaTop/assignments/
echo
curl -s -L -H "Content-type:application/json" -H "X-Dataverse-key:$umaKey" -X POST -d @${_dataDir}/dv-uma-sub1.json "$SERVER/dataverses/umaTop"
echo
curl -s -L -H "Content-type:application/json" -H "X-Dataverse-key:$umaKey" -X POST -d @${_dataDir}/dv-uma-sub2.json "$SERVER/dataverses/umaTop"
echo
