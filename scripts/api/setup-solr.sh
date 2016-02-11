#!/bin/bash

## source-ing setup-getopts.sh provides a common set of shell parameter parsing OPT_* variables for dataverse/environment configuration
. "./setup-getopts.sh"



SOLR_URI="${OPT_u}://${OPT_h}:${OPT_p}/solr/${OPT_c}"
echo "Deleting all index data for Solr Collection $OPT_c at $SOLR_URI"
#curl ${SOLR_URI}/update?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"

