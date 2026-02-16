This release introduces an additial setting related to archival bag creation, ArchiveOnlyIfEarlierVersionsAreArchived (default false). 
If it is true, dataset versions must be archived in order. That is, all prior versions of a dataset must be archived before the latest version can be archived.
This is intended to support use cases where deduplication of files between dataset versions will be done (i.e. by a third-party service running at the archival copy location) and is a step towards supporting the Oxford Common File Layout (OCFL) as an archival format.
