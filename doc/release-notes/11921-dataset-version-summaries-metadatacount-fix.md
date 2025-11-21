### Dataset version summaries API changedFileMetaData count fix

The endpoint ``{id}/versions/compareSummary`` was previously returning an incorrect count for
the ``changedFileMetaData`` field.
The logic for calculating this count has been fixed to accurately reflect the total number of file metadata changes
across all files in the dataset version.

### Related issues

- https://github.com/IQSS/dataverse/issues/11921
