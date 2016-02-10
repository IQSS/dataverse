#!/bin/bash

## setup-getopts.sh provides a common set of shell parameter parsing OPT_* variables for dataverse/environment configuration
SOURCE "./setup-getopts.sh"

SOLR_HOST = 
echo "Deleting all index data for Solr Collection $OPT_c at $SOLR_HOST"
#curl http://localhost:8983/solr/update/json?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"

