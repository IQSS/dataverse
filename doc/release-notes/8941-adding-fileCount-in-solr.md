## Release Highlights

### Adding fileCount as SOLR field

A new search field called `fileCount` can be searched to discover the number of files per dataset. (#10598)

## Upgrade Instructions

1. Update your Solr `schema.xml` to include the new field.
For details, please see https://guides.dataverse.org/en/latest/admin/metadatacustomization.html#updating-the-solr-schema

2. Reindex Solr.
Once the schema.xml is updated, Solr must be restarted and a reindex initiated.
For details, see https://guides.dataverse.org/en/latest/admin/solr-search-index.html but here is the reindex command:
`curl http://localhost:8080/api/admin/index`
