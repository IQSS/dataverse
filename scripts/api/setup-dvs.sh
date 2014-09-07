#!/bin/bash -f
SERVER=http://localhost:8080/api
echo Setting up dataverses on $SERVER
echo ==============================================
if [ $# -eq 0 ]
  then
    echo "Please supply Pete and Uma's API keys like so:"
    echo "$0 [pete's key] [uma's key]"
    echo "The keys are printed at the end of the setup-users.sh script"
    echo "Or, just get them from the database"
    exit 1
fi
echo root
curl -s -H "Content-type:application/json" -X POST -d @data/dv-root.json "$SERVER/dvs/?key=$1"
echo

echo Pete
curl -s -H "Content-type:application/json" -X POST -d @data/dv-pete-top.json "$SERVER/dvs/root?key=$1"
echo
curl -s -H "Content-type:application/json" -X POST -d @data/dv-pete-sub-normal.json "$SERVER/dvs/peteTop?key=$1"
echo
curl -s -H "Content-type:application/json" -X POST -d @data/dv-pete-sub-restricted.json "$SERVER/dvs/peteTop?key=$1"
echo
curl -s -H "Content-type:application/json" -X POST -d @data/dv-pete-sub-secret.json "$SERVER/dvs/peteTop?key=$1"
echo

echo Uma
echo Pete creates top-level for Uma
curl -s -H "Content-type:application/json" -X POST -d @data/dv-uma-top.json "$SERVER/dvs/root?key=$1"
echo
echo Pete makes Uma a manager on her own DV
curl -s -H "Content-type:application/json" -X POST -d"{\"assignee\":\"@uma\",\"role\":\"manager\"}" $SERVER/dvs/umaTop/assignments/?key=$1
echo
curl -s -H "Content-type:application/json" -X POST -d @data/dv-uma-sub1.json "$SERVER/dvs/umaTop?key=$2"
echo
curl -s -H "Content-type:application/json" -X POST -d @data/dv-uma-sub2.json "$SERVER/dvs/umaTop?key=$2"
echo

echo Assign sensible role for the guest on root
curl -s -H "Content-type:application/json" -X POST -d @data/role-guest.json "$SERVER/dvs/root/roles?key=$1"
echo
curl -s -H "Content-type:application/json" -X POST -d"{\"assignee\":\":Guest\",\"role\":\"guest-role\"}" $SERVER/dvs/root/assignments/?key=$1
echo

echo Set the metadata block for Root
curl -s -X POST -H "Content-type:application/json" -d "[\"citation\"]" $SERVER/dvs/:root/metadatablocks/?key=$1


