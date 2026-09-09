A new configuration setting has been introduced for S3 compatible storage drivers that addresses an incompatibility between the AWS S3 library used in Dataverse and certain S3 implementations such as the Ceph Object Gateway:
`dataverse.files.<id>.disable-multipart-download-for-indirect-download` (default: `false`).

When set to `true`, multipart download is disabled for the specified S3 driver, forcing the server to handle part reassembly and avoiding the incompatible
headers. This is recommended for Ceph-backed S3 storage if `412` errors are encountered during download when `dataverse.files.<id>.download-redirect` is set to `false`.
