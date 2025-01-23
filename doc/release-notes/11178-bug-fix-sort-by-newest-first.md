### Bug fix: Sorting by "newest first"

Fixed an issue where draft and minor versions of datasets were sorted using the release timestamp of their most recent major version.
This caused newer drafts or minor versions to appear incorrectly alongside their corresponding major version, instead of at the top, when sorted by "newest first".
Sorting now consistently uses the last update timestamp for all dataset versions (draft, minor, and major).

**Upgrade instructions**: all datasets must be reindexed for this fix to take effect.