#!/bin/sh
curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @data/metadatablocks/biomedical2.tsv -H "Content-type: text/tab-separated-values"