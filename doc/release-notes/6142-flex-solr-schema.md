### Upgrading: optionally reconfigure Solr

With this release, we moved all fields in Solr search index that relate to the default metadata schemas to separate
files. Custom metadata block configuration of the search index is getting more flexible that way. For details, 
see admin/metadatacustomization.html#updating-the-solr-schema.

This is optional, but all future changes will go to these files. It might be a good idea to adapt now or be aware to
look for changes to these files in the future, too.

**When you do want to benefit as of now:**

1. You will need to replace or modify your `schema.xml` with the recent one (containing XML includes)
2. Copy the schema_dv_mdb_XXX.xml files to the same location as the `schema.xml`
3. A re-index is not necessary as long no other changes happened, as this is only a configuration moving.

In case you use custom metadata blocks, you might find the new `updateSchemaMDB.sh` script beneficial. Again,
see admin/metadatacustomization.html#updating-the-solr-schema.