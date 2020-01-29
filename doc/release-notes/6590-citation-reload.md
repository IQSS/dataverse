5. Update Citation Metadata Block

- `wget https://github.com/IQSS/dataverse/releases/download/$releasenumber/citation.tsv`
- `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @citation.tsv -H "Content-type: text/tab-separated-values"`