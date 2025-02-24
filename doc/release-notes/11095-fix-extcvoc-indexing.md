Some External Controlled Vocabulary scripts/configurations, when used on a metadata field that is single-valued could result 
in indexing failure for the dataset (e.g. when the script tried to index both the identifier and name of the identified entity for indexing).
Dataverse has been updated to correctly indicate the need for a multi-valued Solr field in these cases in the call to /api/admin/index/solr/schema.
Configuring the Solr schema and the update-fields.sh script as usually recommended when using custom metadata blocks will resolve the issue.

The overall release notes should include a Solr update (which hopefully is required by an update to 9.7.0 anyway) and our standard instructions 
should change to recommending use of the update-fields.sh script when using custom metadatablocks *and/or external vocabulary scripts*.
