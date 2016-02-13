#!/bin/bash

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "./bin/dataverse-getopts.sh"

SERVER="http://${OPT_h}:8080/api"

if [ -z ${QUIETMODE+x} ] || [ $QUIETMODE -ne "" ]; then 
  CURL_CMD='curl -s'
  CURL_STDOUT='-o /dev/null'
else
  CURL_CMD='curl'
  CURL_STDOUT=''
fi

$CURL_CMD ${SERVER}/admin/datasetfield/loadNAControlledVocabularyValue $CURL_STDOUT
$CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/citation.tsv -H "Content-type: text/tab-separated-values" $CURL_STDOUT
$CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/geospatial.tsv -H "Content-type: text/tab-separated-values" $CURL_STDOUT
$CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/social_science.tsv -H "Content-type: text/tab-separated-values" $CURL_STDOUT
$CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/astrophysics.tsv -H "Content-type: text/tab-separated-values" $CURL_STDOUT
$CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/biomedical.tsv -H "Content-type: text/tab-separated-values" $CURL_STDOUT
$CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/journals.tsv -H "Content-type: text/tab-separated-values" $CURL_STDOUT
