#!/bin/bash

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "./bin/dataverse-getopts.sh"

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
. "./bin/util-set-verbosity.sh"

$_IF_TERSE echo "Configuring solr to index dataverse collection: ${OPT_c} ..."

DATAVERSE_URI="http://${OPT_h}:8080/api"
SOLR_URL_SCHEMA="http://"
if [[ -z ${OPT_z} ]]; then 
  ## Undefined OPT_z mean this is a standard single-host (possibly remote) solr core setup
  if [[ ( -n ${OPT_u} ) && (${OPT_u} != 0) ]]; then
    ## Non-zero OPT_u means this solr server expects an https url schema
    $_IF_VERBOSE echo "Setting dataverse servers useSolrViaHTTPS property to 'yes'"
    $_IF_VERBOSE curl -X PUT -d 'yes' ${DATAVERSE_URI}/admin/settings/:useSolrViaHTTPS
    SOLR_URL_SCHEMA="https://"
  fi
  SOLR_URI="${SOLR_URL_SCHEMA}${OPT_s}:${OPT_p}/solr/${OPT_c}"
  $_IF_VERBOSE echo "Deleting all index data for Solr Collection $OPT_c at $SOLR_URI"
  $_IF_VERBOSE curl -k ${SOLR_URI}/update?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"
  $_IF_VERBOSE echo "Setting dataverse servers SolrHostColonPort property to ${OPT_s}:${OPT_p}"
  $_IF_VERBOSE curl -X PUT -d ${OPT_s}:${OPT_p} ${DATAVERSE_URI}/admin/settings/:SolrHostColonPort
else
  $_IF_VERBOSE echo "Setting dataverse servers SolrZookeeperEnsemble property to '${OPT_z}/solr'"
  $_IF_VERBOSE curl -X PUT -d "${OPT_z}/solr" ${DATAVERSE_URI}/admin/settings/:SolrZookeeperEnsemble
  $_IF_VERBOSE echo "Setting dataverse servers useSolrCloudViaZookeeper property to 'yes'"
  $_IF_VERBOSE curl -X PUT -d 'yes' ${DATAVERSE_URI}/admin/settings/:useSolrCloudViaZookeeper
fi

$_IF_VERBOSE echo "Setting dataverse servers SolrCollectionName property to '${OPT_c}'"
$_IF_VERBOSE curl -X PUT -d "${OPT_c}" ${DATAVERSE_URI}/admin/settings/:SolrCollectionName
$_IF_VERBOSE echo ""