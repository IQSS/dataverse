The dataset storage cleanup call is now `PUT /api/datasets/{id}/cleanStorage`. It was a `GET`, which meant anything that follows links (browser prefetch, link previews in chat and mail clients, crawlers, or revisiting the URL from history) could trigger a deletion.

Two further changes make it safer:

- `dryrun` now defaults to `true`. Omitting it reports what would be removed instead of removing it. Pass `dryrun=false` to actually delete.
- Storage objects modified more recently than `dataverse.files.clean-storage-min-age-days` (7 by default) are never removed. An upload is written to the dataset's storage location before Dataverse registers it as a file, so without a grace period a completed upload still waiting to be saved was indistinguishable from an abandoned one and could be deleted. Raise the setting if uploads in your installation stay unregistered for longer than a week.

Scripts calling this endpoint need to switch to `PUT` and, if they relied on the old default to delete, to pass `dryrun=false` explicitly.
