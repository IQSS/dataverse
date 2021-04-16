#!/usr/bin/env bash

# publish dataset based on database id

dset_id=$1
if [ -z "$dset_id" ]; then
	echo "no dataset id specified, bailing out"
	exit 1
fi

k_d=burrito

h=http://dvsrv

curl -X POST -H "X-Dataverse-key: ${k_d}" "${h}/api/datasets/${dset_id}/actions/:publish?type=major"


