### URL signing API fixed and URL Signing hardened

The `/api/admin/requestSignedUrl` API works again for URLs containing special characters such as the `:` in `persistentId` query parameters. This restores functionality accidentally broken in Dataverse 6.10.

URL Signing has also been improved. When the optional `dataverse.api.signing-secret` JVM option is not set, a strong signing secret is generated automatically at startup (kept only in memory and never stored on disk).

There are limitations to using a generated secret - signed URLs cannot be validated after a server restart, and, on a multi-server installation, signed URLs created on one server cannot be validated on another, resulting in failures if a user's activities cross servers. Installations running multiple servers or that want signed URLs to be valid across server restarts should set `dataverse.api.signing-secret` (a minimum of 36 characters is now required, shorter values are ignored with a warning in the Payara `server.log`). See [dataverse.api.signing-secret](https://guides.dataverse.org/en/latest/installation/config.html#dataverse-api-signing-secret) in the Configuration Guide.
