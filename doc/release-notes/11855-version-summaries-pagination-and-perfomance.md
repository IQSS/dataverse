### Pagination for API Version Summaries

We've added pagination support to the following API endpoints:

- File Version Differences: api/files/{id}/versionDifferences

- Dataset Version Summaries: api/datasets/:persistentId/versions/compareSummary

You can now use two new query parameters to control the results:

- **limit**: An integer specifying the maximum number of results to return per page.

- **offset**: An integer specifying the number of results to skip before starting to return items. This is used to navigate to different pages.

### Performance enhancements for API Version Summaries

In addition to adding pagination, we've significantly improved the performance of these endpoints by implementing more efficient database queries.

These changes address performance bottlenecks that were previously encountered, especially with datasets or files containing a large number of versions.

### Related issues and PRs

- https://github.com/IQSS/dataverse/issues/11855
- https://github.com/IQSS/dataverse/pull/11859
