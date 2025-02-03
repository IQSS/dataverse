### Bug fix: Sorting by "newest first"

Fixed an issue where draft versions of datasets were sorted using the release timestamp of their most recent major version.
This caused newer drafts to appear incorrectly alongside their corresponding major version, instead of at the top, when sorted by "newest first".
Sorting now uses the last update timestamp when sorting draft datasets.
The sorting behavior of published dataset versions (major and minor) is unchanged.

**Upgrade instructions**: draft datasets must be reindexed for this fix to take effect.