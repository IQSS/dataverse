### New keywordTermURI Metadata in keyword Metadata Block

Adding a new metadata `keywordTermURI` to the `keyword` metadata block to facilitate the integration of controlled vocabulary services, in particular by adding the possibility of saving the "term" and its associated URI. (Issue #10288)

## Upgrade Instructions

1\. Update the Citation metadata block

- `wget https://github.com/IQSS/dataverse/releases/download/v6.2/citation.tsv`
- `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @citation.tsv -H "Content-type: text/tab-separated-values"`

2\. Update your Solr `schema.xml` to include the new field.

   For details, please see https://guides.dataverse.org/en/latest/admin/metadatacustomization.html#updating-the-solr-schema


3\. Reindex Solr.
   
   Once the schema.xml is updated, Solr must be restarted and a reindex initiated.
   For details, see https://guides.dataverse.org/en/latest/admin/solr-search-index.html but here is the reindex command:

   `curl http://localhost:8080/api/admin/index`


4\. Run ReExportAll to update dataset metadata exports. Follow the instructions in the [Metadata Export of Admin Guide](https://guides.dataverse.org/en/latest/admin/metadataexport.html#batch-exports-through-the-api).


## Notes for Dataverse Installation Administrators

### Data migration to the new `keywordTermURI` field

You can migrate your `keywordValue` data containing URIs to the new `keywordTermURI` field.
In case of data migration, view the affected data with the following database query:

```
SELECT value FROM datasetfieldvalue dfv
INNER JOIN datasetfield df ON df.id = dfv.datasetfield_id 
WHERE df.datasetfieldtype_id = (SELECT id FROM datasetfieldtype WHERE name = 'keywordValue')
AND value ILIKE 'http%';
```

If you wish to migrate your data, a database update is then necessary:

```
UPDATE datasetfield df
SET datasetfieldtype_id  = (SELECT id FROM datasetfieldtype WHERE name = 'keywordTermURI')
FROM datasetfieldvalue dfv
WHERE dfv.datasetfield_id  = df.id 
AND df.datasetfieldtype_id = (SELECT id FROM datasetfieldtype WHERE name = 'keywordValue')
AND dfv.value ILIKE 'http%';
```

A ['Reindex in Place'](https://guides.dataverse.org/en/latest/admin/solr-search-index.html#reindex-in-place) will be required and ReExportAll will need to be run to update the metadata exports of the dataset. Follow the directions in the [Admin Guide](http://guides.dataverse.org/en/latest/admin/metadataexport.html#batch-exports-through-the-api).