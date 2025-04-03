### Solr Indexing speed improved

The performance of Solr indexing has been significantly improved, particularly for datasets with many files.

A new dataverse.solr.min-files-to-use-proxy microprofile setting can be used to further improve performance/lower memory requirements for datasets with many files (e.g. 500+) (defaults to Integer.MAX, disabling use of the new functionality)
