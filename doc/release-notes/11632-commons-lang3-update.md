Due to changes in how the commons-lang3 library handles a non-ascii chararacter, two keys in the citation.properties and citation.tsv files have changed to include i instead of ɨ. Translations will need to address this.

controlledvocabulary.language.magɨ_(madang_province) => controlledvocabulary.language.magi_(madang_province)
controlledvocabulary.language.magɨyi =>  controlledvocabulary.language.magiyi

## Upgrade Instructions

x\. Update metadata blocks

These changes reflect incremental improvements made to the handling of core metadata fields.

Reload the citation.tsv file to handle the commons-lang3 change mentioned above.

Expect the loading of the citation block to take several seconds because of its size (especially due to the number of languages).

```shell
wget https://raw.githubusercontent.com/IQSS/dataverse/v6.8/scripts/api/data/metadatablocks/citation.tsv

curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file citation.tsv
```
