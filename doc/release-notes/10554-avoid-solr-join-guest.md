An experimental feature flag called "avoid-expensive-solr-join" has been added to change the way Solr queries are constructed for guest (unauthenticated) users. It is hoped that it will help with performance, reducing load on Solr.

From a search perspective, it disables IP Groups (collections, datasets, and files will not be discoverable) but it removes an expensive Solr join for the most common users, which are guests. After turning on this feature, you must perform a full reindex.
