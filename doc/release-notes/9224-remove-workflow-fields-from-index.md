# Optional: remove Workflow Schema fields from Solr index

In Dataverse 5.12 we added a new experimental metadata schema block for workflow deposition.
We included the fields within the standard Solr schema we provide. With this version, we
removed it from the schema. If you are deploying the block to your installation, make sure to
update your index.

If you already added these fields, you can delete them from your index when not using the schema.
Make sure to [reindex after changing the schema](https://guides.dataverse.org/en/latest/admin/solr-search-index.html?highlight=reindex#reindex-in-place.

Remember: depending on the size of your installation, reindexing may take serious time to complete.
You should do this in off-hours.