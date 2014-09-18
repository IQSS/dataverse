#!/bin/bash -f
echo root
curl -H "Content-type:application/json" -X POST -d @data/dv-root.json "http://localhost:8080/api/dvs/?key=pete"
echo

echo Pete
curl -H "Content-type:application/json" -X POST -d @data/dv-pete-top.json "http://localhost:8080/api/dvs/root?key=pete"
echo
curl -H "Content-type:application/json" -X POST -d @data/dv-pete-sub-normal.json "http://localhost:8080/api/dvs/peteTop?key=pete"
echo
curl -H "Content-type:application/json" -X POST -d @data/dv-pete-sub-restricted.json "http://localhost:8080/api/dvs/peteTop?key=pete"
echo
curl -H "Content-type:application/json" -X POST -d @data/dv-pete-sub-secret.json "http://localhost:8080/api/dvs/peteTop?key=pete"
echo

echo Uma
echo Pete creates top-level for Uma
curl -H "Content-type:application/json" -X POST -d @data/dv-uma-top.json "http://localhost:8080/api/dvs/root?key=pete"
echo
echo Pete makes Uma a manager on her own DV
curl -H "Content-type:application/json" -X POST -d"{\"userName\":\"uma\",\"roleAlias\":\"manager\"}" http://localhost:8080/api/dvs/umaTop/assignments/?key=pete
echo
curl -H "Content-type:application/json" -X POST -d @data/dv-uma-sub1.json "http://localhost:8080/api/dvs/umaTop?key=uma"
echo
curl -H "Content-type:application/json" -X POST -d @data/dv-uma-sub2.json "http://localhost:8080/api/dvs/umaTop?key=uma"
echo

echo Assign sensible role for the guest on root
curl -H "Content-type:application/json" -X POST -d @data/role-guest.json "http://localhost:8080/api/dvs/root/roles?key=pete"
curl -H "Content-type:application/json" -X POST -d"{\"userName\":\"__GUEST__\",\"roleAlias\":\"guest-role\"}" http://localhost:8080/api/dvs/root/assignments/?key=pete

echo Set the metadata block for Root
curl -X POST -H "Content-type:application/json" -d "[\"citation\"]" http://localhost:8080/api/dvs/:root/metadatablocks/?key=pete


