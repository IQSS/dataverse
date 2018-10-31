You must run the following curl command:

`curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file scripts/api/data/metadatablocks/citation.tsv`

The reason is that we have added "identifier" values to the "contributorType" controlled vocabulary values so that we can target "Funder" without using the English word.
