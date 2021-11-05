## Upgrade Instructions

X. Reload Citation Metadata Block:

   `wget https://github.com/IQSS/dataverse/releases/download/v5.X/citation.tsv`
   `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @citation.tsv -H "Content-type: text/tab-separated-values"`