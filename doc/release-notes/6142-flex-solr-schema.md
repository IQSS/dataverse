### Upgrading: optionally reconfigure Solr

With this release, we moved all fields in Solr search index that relate to the default metadata schemas from `schema.xml` to separate
files. Custom metadata block configuration of the search index can be more easily automated that way. For details, 
see admin/metadatacustomization.html#updating-the-solr-schema.

This is optional, but all future changes will go to these files. It might be a good idea to reconfigure Solr now or be aware to
look for changes to these files in the future, too. Here's how:

1. You will need to replace or modify your `schema.xml` with the recent one (containing XML includes)
2. Copy `schema_dv_mdb_fields.xml` and `schema_dv_mdb_copies.xml` to the same location as the `schema.xml`
3. A re-index is not necessary as long no other changes happened, as this is only a reorganization of Solr fields from a single schema.xml file into multiple files.

In case you use custom metadata blocks, you might find the new `updateSchemaMDB.sh` script beneficial. Again,
see admin/metadatacustomization.html#updating-the-solr-schema.
