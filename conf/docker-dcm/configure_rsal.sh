#!/bin/sh

fn=rsal-workflow2.json
# needs an actual IP (vs a hostname) for whitelist
rsalip=`dig +short rsalsrv`

# create workflow
curl -s -X POST -H "Content-type: application/json" -d @${fn} "http://localhost:8080/api/admin/workflows" 

# put rsal on the whitelist
curl -X PUT -d "127.0.0.1;${rsalip}" "http://localhost:8080/api/admin/workflows/ip-whitelist"

# set workflow as default
curl -X PUT -d "1" "http://localhost:8080/api/admin/workflows/default/PrePublishDataset"

# local access path
curl -X PUT -d "/hpc/storage" "http://localhost:8080/api/admin/settings/:LocalDataAccessPath"

# storage sites
curl -X POST -H "Content-type: application/json" --upload-file site-primary.json "http://localhost:8080/api/admin/storageSites"
curl -X POST -H "Content-type: application/json" --upload-file site-remote.json "http://localhost:8080/api/admin/storageSites"
