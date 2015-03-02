#!/bin/sh
curl http://localhost:8080/api/datasetfield/loadNAControlledVocabularyValue
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/citation.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/geospatial.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/social_science.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/astrophysics.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/biomedical.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/journals.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customMRA.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customGSD.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customARCS.tsv -H "Content-type: text/tab-separated-values"
curl http://localhost:8080/api/datasetfield/load -X POST --data-binary @data/metadatablocks/customPSRI.tsv -H "Content-type: text/tab-separated-values"