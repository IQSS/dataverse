This release fixes a bug that caused Dataverse to generate unnecessary solr documents for files when a file is added/deleted from a draft dataset. These documents could accumulate and potentially impact performance.

Assuming the upgrade to solr 9.8.0 also occurs in this release, there's nothing else needed for this PR. (Starting with a new solr insures the solr db is empty and that a reindex is already required.)


