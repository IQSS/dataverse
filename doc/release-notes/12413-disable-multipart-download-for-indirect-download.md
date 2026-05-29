A new configuration setting has been introduced for S3 compatible storage drivers:
`dataverse.files.<id>.disable-multipart-download-for-indirect-download` (default: `false`).

When set to `true`, multipart download is disabled for the specified S3 driver, forcing the server to handle part reassembly and avoiding the incompatible
headers. This is recommended for Ceph-backed S3 storage if `412` errors are encountered during download.
