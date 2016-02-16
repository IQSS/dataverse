#!/bin/bash

#### Parse dataverse command line options
## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "./bin/dataverse-getopts.sh"

#### Parse output verbosity ####
## *_CMD and _IF_TERSE, _IF_INFO, _IF_VERBOSE command variables are set in 
## /dataverse/scripts/api/bin/util-set-verbosity.sh
. "./bin/util-set-verbosity.sh"

$_IF_TERSE echo "Configuring solr to index dataverse collection: ${OPT_c} ..."


SOLR_URI="${OPT_u}://${OPT_h}:${OPT_p}/solr/${OPT_c}"


echo "Deleting all index data for Solr Collection $OPT_c at $SOLR_URI"
#curl ${SOLR_URI}/update?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"

