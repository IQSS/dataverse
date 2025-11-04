### Fixed: Permissions for `/{identifier}/templates` endpoint

The `/{identifier}/templates` endpoint previously required **`editDataverse`** permissions to retrieve the list of dataverse templates.

This has been corrected: the endpoint now requires **`addDataset`** permissions instead.

**Impact:**
- The endpoint now works in scenarios such as the **Create Dataset form** in the SPA UI, without needing unnecessary elevated permissions.  

Related issues: #11796