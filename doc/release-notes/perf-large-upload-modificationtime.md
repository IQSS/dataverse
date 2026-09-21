### Performance: Prevent $O(N)$ database updates during file uploads on large datasets

#### Summary
Fixed an $O(N)$ performance degradation during batch/sequential file uploads where `UpdateDatasetVersionCommand` unconditionally touched the `modificationTime` of every existing `DataFile` in the dataset.

In datasets with thousands of files (e.g. 14,000+ files), mutating every file's modification timestamp on each upload caused JPA dirty checking to execute an `UPDATE DVOBJECT SET MODIFICATIONTIME = ...` statement for each existing file on every flush ($\frac{N(N+1)}{2}$ cumulative updates). This caused upload times to degrade from 1s/file to over 30s/file.

`UpdateDatasetVersionCommand` now only initializes timestamps on newly created files (`createDate == null`), safely handles null values on existing files, or updates single files when explicit variable metadata (`fmVarMet`) is modified.

#### Community Reference
- Google Groups: https://groups.google.com/g/dataverse-community/c/pzSjF1YPaJw/m/bQWX3W_kBwAJ
