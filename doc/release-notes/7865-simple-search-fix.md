### Simple Search Fix for Solr Configuration

The introduction in v4.17 of a schema_dv_mdb_copies.xml file as part of the Solr configuration accidentally removed the contents of most metadata fields from index used for simple searches in Dataverse (i.e. when one types a word without indicating which field to search in the normal search box). This was somewhat ameliorated/hidden by the fact that many common fields such as description were still included by other means.

This release removes the schema_dv_mdb_copies.xml file and includes the updates needed in the schema.xml file. Installations with no custom metadata blocks can simply replace their current schema.xml file for solr, restart Solr, and run a ['Reindex in Place' as described in the guides](https://guides.dataverse.org/en/latest/admin/solr-search-index.html#reindex-in-place).

Installations using custom metadata blocks should manually copy the contents of their schema_dv_mdb_copies.xml file (excluding the enclosing `<schema>` element and only including the `<copyField>` elements) into their schema.xml file, replacing the section between 

`<!-- Dataverse copyField from http://localhost:8080/api/admin/index/solr/schema -->`
and

`<!-- End: Dataverse-specific -->`.

In existing schema.xml files, this section currently includes only one line: 

`<xi:include href="schema_dv_mdb_copies.xml" xmlns:xi="http://www.w3.org/2001/XInclude" />`.

In this release, that line has already been replaced with the default set of `<copyFields>`. 
It doesn't matter whether schema_dv_mdb_copies.xml was originally created manually or via the recommended updateSchemaMDB.sh script and this fix will work with all prior versions of Dataverse from v4.17 on. If you make further changes to metadata blocks in your installation, you can repeat this process (i.e. run updateSchemaMDB.sh, copy the entries in schema_dv_mdb_copies.xml into the same section of schema.xml, restart solr, and reindex.)

Once schema.xml is updated, solr should be restarted and a ['Reindex in Place'](https://guides.dataverse.org/en/latest/admin/solr-search-index.html#reindex-in-place) will be required. (Future Dataverse versions will avoid this manual copy step.)