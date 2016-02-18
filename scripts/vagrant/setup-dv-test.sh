#!/bin/bash
command -v jq >/dev/null 2>&1 || { echo >&2 '`jq` ("sed for JSON") is required, but not installed. Download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your $PATH (/usr/bin/jq is fine) and executable with `sudo chmod +x /usr/bin/jq`. On Mac, you can install it with `brew install jq` if you use homebrew: http://brew.sh . Aborting.'; exit 1; }

if [[ -n ${1} ]]; then 
  SERVER=http://${1}:8080/api
else
  SERVER=http://192.168.20.20:8080/api
fi


echo Publishing 'curator-able' root dataverse on $SERVER
echo ====================================================================
adminResp=$(curl -L -X GET "$SERVER/builtin-users/dataverseAdmin/api-token?password=admin")
echo $adminResp
adminKey=$(echo $adminResp | jq .data.message | tr -d \")
echo :result: dataverseAdmin\'s key is: $adminKey

curl -X POST -H "X-Dataverse-key:$adminKey" -H "Content-type:application/json"  -d "{\"assignee\":\":authenticated-users\",\"role\":\"dvContributor\"}" ${SERVER}/dataverses/root/assignments
curl -X POST -H "X-Dataverse-key:$adminKey" ${SERVER}/dataverses/root/actions/:publish


echo
echo Setting up users on $SERVER
echo ==============================================

curl -L -X PUT -d burrito $SERVER/admin/settings/BuiltinUsers.KEY

peteResp=$(curl -s -L -H "Content-type:application/json" -X POST -d @/dataverse/scripts/api/data/userPete.json "$SERVER/builtin-users?password=pete&key=burrito")
echo $peteResp

umaResp=$(curl -s -L -H "Content-type:application/json" -X POST -d @/dataverse/scripts/api/data/userUma.json "$SERVER/builtin-users?password=uma&key=burrito")
echo $umaResp

curl -s -L -H "Content-type:application/json" -X POST -d @/dataverse/scripts/api/data/userGabbi.json "$SERVER/builtin-users?password=gabbi&key=burrito"
echo

curl -s -L -H "Content-type:application/json" -X POST -d @/dataverse/scripts/api/data/userCathy.json "$SERVER/builtin-users?password=cathy&key=burrito"
echo

curl -s -L -H "Content-type:application/json" -X POST -d @/dataverse/scripts/api/data/userNick.json "$SERVER/builtin-users?password=nick&key=burrito"
echo

peteKey=$(echo $peteResp | jq .data.apiToken | tr -d \")
echo :result: Pete\'s key is: $peteKey
umaKey=$(echo $umaResp | jq .data.apiToken | tr -d \")
echo :result: Uma\'s key is: $umaKey#!/bin/bash -f


echo
echo Setting up dataverses on $SERVER
echo ==============================================

echo Pete
curl -s -L -H "Content-type:application/json" -X POST -d @/dataverse/scripts/api/data/dv-pete-top.json "$SERVER/dataverses/root?key=$peteKey"
echo
curl -s -L -H "Content-type:application/json" -X POST -d @/dataverse/scripts/api/data/dv-pete-sub-normal.json "$SERVER/dataverses/peteTop?key=$peteKey"
echo
curl -s -L -H "Content-type:application/json" -X POST -d @/dataverse/scripts/api/data/dv-pete-sub-restricted.json "$SERVER/dataverses/peteTop?key=$peteKey"
echo
curl -s -L -H "Content-type:application/json" -X POST -d @/dataverse/scripts/api/data/dv-pete-sub-secret.json "$SERVER/dataverses/peteTop?key=$peteKey"
echo

echo Uma
echo Pete creates top-level for Uma
curl -s -L -H "Content-type:application/json" -H "X-Dataverse-key:$peteKey" -X POST -d @/dataverse/scripts/api/data/dv-uma-top.json "$SERVER/dataverses/root"
echo
echo Pete makes Uma an admin on her own DV
curl -s -L -H "Content-type:application/json" -H "X-Dataverse-key:$peteKey" -X POST -d"{\"assignee\":\"@uma\",\"role\":\"admin\"}" $SERVER/dataverses/umaTop/assignments/
echo
curl -s -L -H "Content-type:application/json" -H "X-Dataverse-key:$umaKey" -X POST -d @/dataverse/scripts/api/data/dv-uma-sub1.json "$SERVER/dataverses/umaTop"
echo
curl -s -L -H "Content-type:application/json" -H "X-Dataverse-key:$umaKey" -X POST -d @/dataverse/scripts/api/data/dv-uma-sub2.json "$SERVER/dataverses/umaTop"
echo
