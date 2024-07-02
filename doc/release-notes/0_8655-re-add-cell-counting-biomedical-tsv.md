## Release Highlights

### Life Science Metadata

Re-adding value `cell counting` to Life Science metadata block's Measurement Type vocabularies accidentally removed in `v5.1`. 

## Upgrade Instructions

### Update the Life Science metadata block

- `wget https://github.com/IQSS/dataverse/releases/download/v6.3/biomedical.tsv`
- `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @biomedical.tsv -H "Content-type: text/tab-separated-values"`