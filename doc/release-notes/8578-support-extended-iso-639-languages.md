The Controlled Vocabulary Values list for the metadata field Language in the Citation block can now be extended to include roughly 7920 ISO 639-3 values.

To be added to the 6.4 release instructions:

Update the Citation block, to incorporate the additional controlled vocabulary for languages:

```
wget https://raw.githubusercontent.com/IQSS/dataverse/v6.4/scripts/api/data/metadatablocks/iso-639-3_Code_Tables_20240415/iso-639-3.tab
curl http://localhost:8080/api/admin/datasetfield/mergeLanguageList -H "Content-type: text/tab-separated-values" -X POST --upload-file iso-639-3.tab
```

