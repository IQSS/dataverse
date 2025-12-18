#!/bin/bash -f
command -v jq >/dev/null 2>&1 || { echo >&2 "jq required, but it's not installed. On mac, use brew (http://brew.sh) to install it. Aborting."; exit 1; }

SERVER=http://localhost:8080/api
echo Setting up users on $SERVER
echo ==============================================

curl -X PUT -d burrito "$SERVER/admin/settings/:BuiltinUsersKey"


peteResp=$(curl -s -H "Content-type:application/json" -X POST -d @data/userPete.json "$SERVER/builtin-users?password=pete&key=burrito")
echo $peteResp

umaResp=$(curl -s -H "Content-type:application/json" -X POST -d @data/userUma.json "$SERVER/builtin-users?password=uma&key=burrito")
echo $umaResp

curl -s -H "Content-type:application/json" -X POST -d @data/userGabbi.json "$SERVER/builtin-users?password=gabbi&key=burrito"
echo

curl -s -H "Content-type:application/json" -X POST -d @data/userCathy.json "$SERVER/builtin-users?password=cathy&key=burrito"
echo

curl -s -H "Content-type:application/json" -X POST -d @data/userNick.json "$SERVER/builtin-users?password=nick&key=burrito"
echo

echo reporting API keys
peteKey=$(echo $peteResp | jq .data.apiToken | tr -d \")
echo :result: Pete\'s key is: $peteKey
umaKey=$(echo $umaResp | jq .data.apiToken | tr -d \")
echo :result: Uma\'s key is: $umaKey