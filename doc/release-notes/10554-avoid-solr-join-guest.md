Two experimental features flag called "add-publicobject-solr-field" and "avoid-expensive-solr-join" have been added to change the way how Solr documents are indexed for public objects, and how Solr queries are constructed to accommodate access to restricted content (drafts, etc.). It is hoped that it will help with performance, especially on large instances and under load.

Before the search feature flag ("avoid-expensive...") can be turned on, the indexing flag must be enabled, and a full reindex performed. Otherwise publicly available objects are NOT going to be shown in search results.
