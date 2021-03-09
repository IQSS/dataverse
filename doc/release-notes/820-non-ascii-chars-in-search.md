(review these notes if this gets into the same release as #7645 as the steps are included there - we expect to include this in the same release)

### Search with non-ascii characters

Many languages include characters that have close analogs in ascii, e.g. (á, à, â, ç, é, è, ê, ë, í, ó, ö, ú, ù, û, ü…). This release changes the default Solr configuration to allow search to match words based on these associations, e.g. a search for Mercè would match the word Merce in a Dataset, and vice versa. This should generally be helpful, but can result in false positives.,e.g. "canon" will be found searching for "cañon". 

## Upgrade Instructions

1. You will need to replace or modify your `schema.xml` and restart solr. Re-indexing is required to get full-functionality from this change - the standard instructions for an incremental reindex could be added here.
   