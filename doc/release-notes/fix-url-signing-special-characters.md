### Signed URLs work again for URLs with special characters

Requesting a signed URL (e.g. via `/api/admin/requestSignedUrl`, used by external tools, the Globus
integration and third-party integrations such as the `rdm-integration` connector) was broken in 6.10
for URLs whose query contained special characters — most notably persistent IDs such as
`doi:10.5072/FK2/ABC` (which contain `:` and `/`), as well as spaces, percent-encoded values and
non-ASCII characters. In 6.10 the signing step began re-encoding/normalizing the URL (for example
percent-encoding `:` and `/`) before computing the signature, while the request is validated against
the URL the caller actually presents back. The re-encoded signature no longer matched, so validation
failed with authentication / "signature does not match" errors.

Signing no longer re-encodes the URL: it is signed exactly as provided, with only the reserved
signing parameters (`until`, `user`, `method`, `token`, `key`, `signed`) stripped out; the rest of
the URL is left untouched, character for character.

**This restores the URL-signing behavior used before 6.10, so it is compatible with older versions
and with existing integrations.** Clients and connectors that build or consume signed URLs the way
they did before 6.10 keep working unchanged, signatures are computed the same way as before the
regression, and URLs containing special characters validate again. No client-side changes are
required.

### A signing secret is now required to request signed URLs

Separately from the fix above, the `/api/admin/requestSignedUrl` endpoint now requires a non-empty
signing secret (`dataverse.api.signing-secret`) to be configured. Previously an unset secret silently
fell back to using only the user's API token as the signing key, which is too weak. If the secret is
not configured, the endpoint now returns an error instead of issuing a weakly-signed URL.

**Upgrade note:** installations that use signed URLs through this endpoint (including the
`rdm-integration` connector) must set `dataverse.api.signing-secret`. See the
[Configuration Guide](https://guides.dataverse.org/en/latest/installation/config.html#dataverse-api-signing-secret).
Treat the value like a password. Because the signing secret is part of the signing key, setting (or
later changing) it invalidates previously issued signed URLs: any existing signed URLs that have not
yet expired will stop working, and clients/integrations will need to request new ones.
