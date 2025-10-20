### Suppression of the Host Dataverse field

When creating a dataset, the _host dataverse_ field is not shown when the user can only add datasets to one collection.

This was introduced in 6.7 with PR #11301 and reverted in 6.7.1 with PR #11700.
A performance penalty was observed in instances with a large number of dataverses.
The performance degradation is remedied by reusing the query for the API command [allowedCollections] but return at most two rows.

allowedCollections: https://guides.dataverse.org/en/latest/api/native-api.html#list-dataverse-collections-a-user-can-act-on-based-on-their-permissions