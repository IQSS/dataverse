The /api/admin/makeDataCount/{id}/updateCitationsForDataset endpoint, which allows citations for a dataset to be retrieved from DataCite, is often called periodically for all datasets. However, allowing calls for many datasets to be processed in parallel can cause performance problems in Dataverse and/or cause calls to DataCite to fail due to rate limiting. The existing implementation was also inefficient w.r.t. memory use when used on datasets with many (>~1K) files. This release configures Dataverse to queue calls to this api, processes them serially, adds optional throttling to avoid hitting DataCite rate limits and improves memory use.

New optional MPConfig setting: 
 
dataverse.api.mdc.min-delay-ms - number of milliseconds to wait between calls to DataCite. A value of ~100 should conservatively address DataCite's current 3000/5 minute limit. A value of 250 may be required for their test service. 

Backward compatibility: This api call is now asynchronous and will return an OK response when the call is queued or a 503 if the queue is full.