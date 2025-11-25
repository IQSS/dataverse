It is now possible to define storage quotas on individual datasets. See the API guide for more information.
The practical use case is for datasets in the top-level, root collection. This does not address the use case of a user creating multiple datasets. But there is an open dev. issue for adding per-user storage quotas as well.

The `/api/datasets/{id}/storageDriver` API has been further extended to (optionally) include the remaining storage and/or number of files quotas, if present.