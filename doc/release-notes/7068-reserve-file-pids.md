## Release Highlights

### Pre-Publish File DOI Reservation with DataCite

Dataverse installations using DataCite (or other persistent identifier (PID) Providers that support reserving PIDs) will be able to reserve PIDs for files when they are uploaded (rather than at publication time). Note that reserving file DOIs can slow uploads with large numbers of files so administrators may need to adjust timeouts (specifically any Apache "``ProxyPass / ajp://localhost:8009/ timeout=``" setting in the recommended Dataverse configuration).

## Major Use Cases

- Users will have DOIs/PIDs reserved for their files as part of file upload instead of at publication time. (Issue #7068, PR #7334)
