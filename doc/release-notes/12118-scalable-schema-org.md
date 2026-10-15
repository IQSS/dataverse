# Scalable Schema.org Export

The Schema.org export (available as an export format and embedded as JSON-LD in dataset page headers) has been improved to better handle datasets with many files.

- For datasets with a large number of files, the export can now use a more scalable `SearchAction` structure instead of listing every individual file in the `distribution` field. This ensures that the metadata remains readable by Google and other search engines without exceeding size limits.
- A new configuration option `dataverse.exports.schema-dot-org.max-files-for-download-entries` has been added to control the threshold for this behavior. By default, all files are included.
