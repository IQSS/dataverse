A new endpoint lists the folder hierarchy of a dataset version one level at a time, with keyset pagination:

`GET /api/datasets/{id}/versions/{versionId}/tree`

See the [Native API guide](https://guides.dataverse.org/en/latest/api/native-api.html) for parameters and the response shape.

## Upgrade instructions

Flyway migration `V6.10.1.2` adds `ix_filemetadata_tree` to keep this endpoint fast. On installations with a large `filemetadata` table, `CREATE INDEX` holds an `ACCESS EXCLUSIVE` lock for the duration of the build, and Flyway cannot use `CONCURRENTLY` inside its transaction. Create it out of band before upgrading:

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_filemetadata_tree
    ON filemetadata (datasetversion_id, directorylabel text_pattern_ops, lower(label), datafile_id);
```

Use that definition exactly. `IF NOT EXISTS` matches by name only, so an index created with a different definition would be kept as is. The migration is a no-op when the index is already there.
