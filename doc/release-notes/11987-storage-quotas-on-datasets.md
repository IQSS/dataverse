It is now possible to define storage quotas on individual datasets. See the API guide for more information.
The practical use case is for datasets in the top-level, root collection. This does not address the use case of a user creating multiple datasets. But there is an open dev. issue for adding per-user storage quotas as well.

A convenience API `/api/datasets/{id}/uploadlimits` has been added to show the remaining storage and/or number of files quotas, if present.