### Storage quotas are now stored directly on collections and datasets

Storage quotas defined on collections and datasets are now stored in a new `storagequota` column in the `dataverse` and `dataset` tables, instead of in a separate `storagequota` table. Existing quotas are copied over automatically by a database migration, and the old table is dropped. The quota APIs are unchanged.

One behavior change: previously, once a quota had been set on a collection or dataset and later deleted, a quota defined on an ancestor collection would no longer apply to it. Now, deleting a quota means the nearest ancestor's quota (if any) applies again, the same as if a quota had never been set there.

See also #12808.
