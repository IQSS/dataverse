### Reindexing after a role assignment is less memory intensive

Adding/removing a user from a role on a collection, particularly the root collection, could lead to a significant increase in memory use resulting in Dataverse itself failing with an out-of-memory condition. Such changes now consume much less memory.

If you have experienced out-of-memory failures in Dataverse in the past that could have been caused by this problem, you may wish to run a [reindex in place](https://guides.dataverse.org/en/latest/admin/solr-search-index.html#reindex-in-place) to update any out-of-date information.

For more information, see #10697 and #10698.
