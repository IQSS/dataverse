#!/bin/sh
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/general.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/social_science.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/astrophysics.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/biomedical.tsv -H "Content-type: text/tab-separated-values"
