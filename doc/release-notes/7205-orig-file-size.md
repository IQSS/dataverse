## Notes to Admins

Beginning in Dataverse Software 4.10, the size of the saved original file (for an ingested tabular datafile) was stored in the database. For files added before this change, we provide an API that retrieves and permanently stores the sizes for any already existing saved originals. See [Datafile Integrity API](https://guides.dataverse.org/en/5.4/api/native-api.html#datafile-integrity) for more information.

This was documented as a step in previous release notes, but we are noting it in these release notes to give it more visibility.

## Upgrade Instructions

X./ Retroactively store original file size

Use the [Datafile Integrity API](https://guides.dataverse.org/en/5.4/api/native-api.html#datafile-integrity) to ensure that the sizes of all original files are stored in the database.