#!/bin/bash

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "./bin/dataverse-getopts.sh"

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
if [[ -e "/dataverse/scripts/api/bin/util-set-verbosity.sh" ]]; then
  . "/dataverse/scripts/api/bin/util-set-verbosity.sh"
elif [[ -e "../../api/bin/util-set-verbosity.sh" ]]; then
  . "../../api/bin/util-set-verbosity.sh"
elif [[ -e "./util-set-verbosity.sh" ]]; then
  . "./util-set-verbosity.sh"
else
  CURL_CMD='curl -s'
  WGET_CMD='wget -q'
  YUM_CMD='yum -q'
  MVN_CMP='mvn -q'
fi

SERVER="http://${OPT_h}:8080/api"

$_IF_INFO echo "Setup the metadata blocks"

$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/loadNAControlledVocabularyValue
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/citation.tsv -H "Content-type: text/tab-separated-values"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/geospatial.tsv -H "Content-type: text/tab-separated-values"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/social_science.tsv -H "Content-type: text/tab-separated-values"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/astrophysics.tsv -H "Content-type: text/tab-separated-values"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/biomedical.tsv -H "Content-type: text/tab-separated-values"
$_IF_VERBOSE $CURL_CMD ${SERVER}/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/journals.tsv -H "Content-type: text/tab-separated-values"
