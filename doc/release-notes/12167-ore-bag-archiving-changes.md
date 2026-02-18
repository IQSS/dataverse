## Archiving, OAI-ORE, and BagIt Export

This release includes multiple updates to the OAI-ORE metadata export and the process of creating archival bags, improving performance, fixing bugs, and adding significant new functionality.

### General Archiving Improvements
- Multiple performance and scaling improvements have been made for creating archival bags for large datasets, including:
  - The duration of archiving tasks triggered from the version table or API are no longer limited by the transaction time limit.
  - Temporary storage space requirements have increased by `1/:BagGeneratorThreads` of the zipped bag size. (This is a consequence of changes to avoid timeout errors on larger files/datasets.)
  - The size of individual data files and the total dataset size that will be included in an archival bag can now be limited. Admins can choose whether files above these limits are transferred along with, but outside, the zipped bag (creating a complete archival copy) or are just referenced (using the concept of a "holey" bag and just listing the oversized files and the Dataverse URLs from which they can be retrieved in a `fetch.txt` file). In the holey bag case, an active service on the archiving platform must retrieve the oversized files (using appropriate credentials as needed) to make a complete copy.
  - Superusers can now see a pending status in the dataset version table while archiving is active.
  - Workflows are now triggered outside the transactions related to publication, assuring that workflow locks and status updates are always recorded.
  - Potential conflicts between archiving/workflows, indexing, and metadata exports after publication have been resolved, avoiding cases where the status/last update times for these actions were not recorded.
- A bug has been fixed where superusers would incorrectly see the "Submit" button to launch archiving from the dataset page version table.
- The local, S3, and Google archivers have been updated to support deleting existing archival files for a version to allow re-creating the bag for a given version.
- For archivers that support file deletion, it is now possible to recreate an archival bag after "Update Current Version" has been used (replacing the original bag). By default, Dataverse will mark the current version's archive as out-of-date, but will not automatically re-archive it.
  - A new 'obsolete' status has been added to indicate when an archival bag exists for a version but it was created prior to an "Update Current Version" change.
- Improvements have been made to file retrieval for bagging, including retries on errors and when download requests are being throttled.
  - A bug causing `:BagGeneratorThreads` to be ignored has been fixed, and the default has been reduced to 2.
- Retrieval of files for inclusion in an archival bag is no longer counted as a download.
- It is now possible to require that all previous versions have been successfully archived before archiving of a newly published version can succeed. (This is intended to support use cases where deduplication of files between dataset versions will be done and is a step towards supporting the Oxford Common File Layout (OCFL).)
- The pending status has changed to use the same JSON format as other statuses

### OAI-ORE Export Updates
- The export now uses URIs for checksum algorithms, conforming with JSON-LD requirements.
- A bug causing failures with deaccessioned versions has been fixed. This occurred when the deaccession note ("Deaccession Reason" in the UI) was null, which is permissible via the API.
- The `https://schema.org/additionalType` has been updated to "Dataverse OREMap Format v1.0.2" to reflect format changes.

### Archival Bag (BagIt) Updates
- The `bag-info.txt` file now correctly includes information for dataset contacts, fixing a bug where nothing was included when multiple contacts were defined. (Multiple contacts were always included in the OAI-ORE file in the bag; only the baginfo file was affected).
- Values used in the `bag-info.txt` file that may be multi-line (i.e. with embedded CR or LF characters) are now properly indented and wrapped per the BagIt specification (`Internal-Sender-Identifier`, `External-Description`, `Source-Organization`, `Organization-Address`).
- The dataset name is no longer used as a subdirectory within the `data/` directory to reduce issues with unzipping long paths on some filesystems.
- For dataset versions with no files, the empty `manifest-<alg>.txt` file will now use the algorithm from the `:FileFixityChecksumAlgorithm` setting instead of defaulting to MD5.
- A new key, `Dataverse-Bag-Version`, has been added to `bag-info.txt` with the value "1.0" to allow for tracking changes to Dataverse's archival bag generation over time.
- When using the `holey` bag option discussed above, the required `fetch.txt` file will be included.


### New Configuration Settings

This release introduces several new settings to control archival and bagging behavior.

- **`dataverse.archive.archive-only-if-earlier-versions-are-archived`** (Default: `false`)
  When set to `true`, dataset versions must be archived in order. That is, all prior versions of a dataset must be archived before the latest version can be archived.

- **`dataverse.feature.archive-on-version-update`** (Default: `false`)
  Indicates whether archival bag creation should be triggered (if configured) when a version is updated and was already successfully archived, i.e., via the Update-Current-Version publication option. Setting the flag to `true` only works if the archiver being used supports deleting existing archival bags.


### Bag Size Control And Holey Bag Support

The following JVM options (MicroProfile Config Settings) control this feature:
- `dataverse.bagit.zip.holey`
- `dataverse.bagit.zip.max-data-size`
- `dataverse.bagit.zip.max-file-size`