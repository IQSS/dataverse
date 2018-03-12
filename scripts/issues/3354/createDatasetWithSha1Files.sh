#!/bin/sh
# existing, works, no files, commenting out
#curl -s -X POST -H "Content-type:application/json" -d @scripts/search/tests/data/dataset-finch1.json "http://localhost:8080/api/dataverses/root/datasets/?key=$API_TOKEN"
# new, has files
curl -s -X POST -H "Content-type:application/json" -d @scripts/issues/3354/datasetWithSha1Files.json "http://localhost:8080/api/dataverses/root/datasets/?key=$API_TOKEN"
