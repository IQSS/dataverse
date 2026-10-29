A new endpoint lists the folder hierarchy of a dataset version one level at a time, with keyset pagination:

`GET /api/datasets/{id}/versions/{versionId}/tree`

See the [Native API guide](https://guides.dataverse.org/en/latest/api/native-api.html) for parameters and the response shape.

## Upgrade instructions

Flyway migration `V6.11.0.2` adds `ix_filemetadata_tree` to keep this endpoint fast. `CREATE INDEX` blocks writes while it builds. On installations with a large `filemetadata` table, create it outside Flyway's transaction before upgrading:

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_filemetadata_tree
    ON filemetadata (datasetversion_id, directorylabel text_pattern_ops, lower(label), datafile_id);
```

Use that definition exactly. `IF NOT EXISTS` matches by name only, so an index created with a different definition would be kept as is. The migration is a no-op when the index is already there.
