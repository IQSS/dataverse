#!/usr/bin/env bash


# user creates dataset
k_d=burrito
dv_d=root
h=http://dvsrv

fn=dataset.json
#dset_id=`curl -s -H "X-Dataverse-key: $k_d" -X POST --upload-file $fn $h/api/dataverses/$dv_d/datasets | jq .data.id`
r=`curl -s -H "X-Dataverse-key: $k_d" -X POST --upload-file $fn $h/api/dataverses/$dv_d/datasets`
echo $r
dset_id=`echo $r | jq .data.id`
echo "dataset created with id: $dset_id"

if [ "null" == "${dset_id}" ]; then
	echo "error - no dataset id from create command"
	exit 1
fi
echo "dataset created; internal/db id: ${dset_id}"


