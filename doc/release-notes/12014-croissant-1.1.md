### Croissant 1.1 (Summary Statistics)

The Croissant metadata export format has been updated from version 1.0 to 1.1.

Summary statistics (mean, min, max, etc.) are now included for tabular files that were successfully ingested.

You can download an example Croissant file from the [Supported Metadata Export Formats](https://dataverse-guide--12214.org.readthedocs.build/en/12214/user/dataset-management.html#supported-metadata-export-formats) section of the guides.

Minor backward-incompatible changes were made, which are noted below.

See #12014 and #12214

## Backward Incompatible Changes

Generally speaking, see the [API Changelog](https://guides.dataverse.org/en/latest/api/changelog.html) for a list of backward-incompatible API changes.

Minor changes in the `croissant` format are noted in the [API changelog](https://dataverse-guide--12214.org.readthedocs.build/en/12214/api/changelog.html).

## Upgrade Instructions

1. Re-export metadata export formats

   We re-export because the Croissant format was updated.

   `curl http://localhost:8080/api/admin/metadata/reExportAll`
