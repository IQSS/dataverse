A features flag called "reduce-solr-deletes" has been added to improve how datafiles are indexed. When the flag is enabled, 
Dataverse wil avoid pre-emptively deleting existing solr documents for the files prior to sending updated information. This
should improve performance and will allow additional optimizations going forward.

The /api/admin/index/status and /api/admin/index/clear-orphans calls
(see https://guides.dataverse.org/en/latest/admin/solr-search-index.html#index-and-database-consistency)
will now find and remove (respectively) additional permissions related solr documents that were not being detected before.
Reducing the overall number of documents will improve solr performance and large sites may wish to periodically call the 
clear-orphans API.