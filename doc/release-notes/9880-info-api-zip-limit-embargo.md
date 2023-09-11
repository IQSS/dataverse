Implemented the following new endpoints:

- getZipDownloadLimit (/api/info/zipDownloadLimit): Get the configured zip file download limit. The response contains the long value of the limit in bytes.

- getMaxEmbargoDurationInMonths (/api/info/settings/:MaxEmbargoDurationInMonths): Get the maximum embargo duration in months, if available, configured through the database setting :MaxEmbargoDurationInMonths.
