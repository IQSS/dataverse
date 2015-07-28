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

echo Pete
curl -s -H "Content-type:application/json" -X POST -d @data/dv-pete-top.json "$SERVER/dataverses/root?key=$1"
echo
curl -s -H "Content-type:application/json" -X POST -d @data/dv-pete-sub-normal.json "$SERVER/dataverses/peteTop?key=$1"
echo
curl -s -H "Content-type:application/json" -X POST -d @data/dv-pete-sub-restricted.json "$SERVER/dataverses/peteTop?key=$1"
echo
curl -s -H "Content-type:application/json" -X POST -d @data/dv-pete-sub-secret.json "$SERVER/dataverses/peteTop?key=$1"
echo

echo Uma
echo Pete creates top-level for Uma
curl -s -H "Content-type:application/json" -H "X-Dataverse-key:$1" -X POST -d @data/dv-uma-top.json "$SERVER/dataverses/root"
echo
echo Pete makes Uma an admin on her own DV
curl -s -H "Content-type:application/json" -H "X-Dataverse-key:$1" -X POST -d"{\"assignee\":\"@uma\",\"role\":\"admin\"}" $SERVER/dataverses/umaTop/assignments/
echo
curl -s -H "Content-type:application/json" -H "X-Dataverse-key:$2" -X POST -d @data/dv-uma-sub1.json "$SERVER/dataverses/umaTop"
echo
curl -s -H "Content-type:application/json" -H "X-Dataverse-key:$2" -X POST -d @data/dv-uma-sub2.json "$SERVER/dataverses/umaTop"
echo
