#!/bin/bash

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "./bin/dataverse-getopts.sh"

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
. "./bin/util-set-verbosity.sh"

SERVER="http://${OPT_h}:8080/api"

$_IF_INFO echo "Setup the metadata blocks"

$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/loadNAControlledVocabularyValue
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/citation.tsv -H "Content-type: text/tab-separated-values"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/geospatial.tsv -H "Content-type: text/tab-separated-values"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/social_science.tsv -H "Content-type: text/tab-separated-values"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/astrophysics.tsv -H "Content-type: text/tab-separated-values"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/biomedical.tsv -H "Content-type: text/tab-separated-values"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/journals.tsv -H "Content-type: text/tab-separated-values"
