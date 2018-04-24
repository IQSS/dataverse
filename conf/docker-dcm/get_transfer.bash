#!/usr/bin/env bash

# user gets transfer script

dset_id=$1
if [ -z "$dset_id" ]; then
	echo "no dataset id specified, bailing out"
	exit 1
fi

k_d=burrito
dv_d=root

h=http://dvsrv

#get upload script from DCM
wget --header "X-Dataverse-key: ${k_d}" ${h}/api/datasets/${dset_id}/dataCaptureModule/rsync -O upload-${dset_id}.bash


