#!/bin/bash

DATAVERSE_URL=${DATAVERSE_URL:-"http://localhost:8080"}
SCRIPT_PATH="$(dirname "$0")"

curl "${DATAVERSE_URL}/api/admin/datasetfield/loadNAControlledVocabularyValue"
# TODO: The "@" is confusing. Consider switching to --upload-file citation.tsv
curl "${DATAVERSE_URL}/api/admin/datasetfield/load" -X POST --data-binary @"$SCRIPT_PATH"/data/metadatablocks/citation.tsv -H "Content-type: text/tab-separated-values"
curl "${DATAVERSE_URL}/api/admin/datasetfield/load" -X POST --data-binary @"$SCRIPT_PATH"/data/metadatablocks/geospatial.tsv -H "Content-type: text/tab-separated-values"
curl "${DATAVERSE_URL}/api/admin/datasetfield/load" -X POST --data-binary @"$SCRIPT_PATH"/data/metadatablocks/social_science.tsv -H "Content-type: text/tab-separated-values"
curl "${DATAVERSE_URL}/api/admin/datasetfield/load" -X POST --data-binary @"$SCRIPT_PATH"/data/metadatablocks/astrophysics.tsv -H "Content-type: text/tab-separated-values"
curl "${DATAVERSE_URL}/api/admin/datasetfield/load" -X POST --data-binary @"$SCRIPT_PATH"/data/metadatablocks/biomedical.tsv -H "Content-type: text/tab-separated-values"
curl "${DATAVERSE_URL}/api/admin/datasetfield/load" -X POST --data-binary @"$SCRIPT_PATH"/data/metadatablocks/journals.tsv -H "Content-type: text/tab-separated-values"
