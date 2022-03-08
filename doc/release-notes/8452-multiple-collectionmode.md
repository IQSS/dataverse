### A small modification to the Social Science metadata block

The metadata block update allows the field "collectionMode" to have multiple values and to support Controlled Vocabularies. 

For the upgrade instruction:

Update the Social Science metadata block as follows:

- `wget https://github.com/IQSS/dataverse/releases/download/v5.10/social_science.tsv`
- `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @social_science.tsv -H "Content-type: text/tab-separated-values"`

As a general reminder, please note that it is important to keep your metadata block definitions up-to-date.