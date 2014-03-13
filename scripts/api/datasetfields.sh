#!/bin/sh
curl http://localhost:8080/api/datasetfield/showtsv -X POST --data-binary @doc/Sphinx/source/User/metadata/general.tsv -H 'Content-type: text/tab-separated-values'
