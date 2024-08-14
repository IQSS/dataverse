The Controlled Vocabulary Values list for the metadata field Language in the Citation block has now been extended to include roughly 7920 ISO 639-3 values.
- Some of the language entries in the pre-v.6.4 list correspond to "macro languages" in ISO-639-3 and admins/users may wish to update to use the corresponding individual language entries from ISO-639-3. As these cases are expected to be rare (they do not involve major world languages), finding them is not covered in the release notes. Anyone who desires help in this area is encouraged to reach out to the Dataverse community via any of the standard communication channels.
- ISO 639-3 codes were downloaded from:
```
https://iso639-3.sil.org/code_tables/download_tables#Complete%20Code%20Tables:~:text=iso%2D639%2D3_Code_Tables_20240415.zip
```
- The file used for merging with the existing citation.tsv was iso-639-3.tab

To be added to the 6.4 release instructions:

### Additional Upgrade Steps
6\. Update the Citation metadata block:

```
- `wget https://github.com/IQSS/dataverse/releases/download/v6.4/citation.tsv`
- `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @citation.tsv -H "Content-type: text/tab-separated-values"`
```
