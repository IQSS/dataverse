### Signed URLs work again for URLs with special characters

Requesting a signed URL (e.g. via `/api/admin/requestSignedUrl`, used by external tools, the Globus
integration and third-party integrations) was broken for URLs whose query contained special
characters; most notably persistent IDs such as `doi:10.5072/FK2/ABC` (which contain `:` and
`/`), as well as spaces, percent-encoded values and non-ASCII characters. The signing logic had
started normalizing/re-encoding the URL before signing it, while the signature is a byte-exact MAC
over the URL string; the re-encoded bytes no longer matched what callers presented back, so
validation failed with "signature does not match"/authentication errors.

Signing is now byte-exact again: the base URL is preserved exactly as provided (reserved signing
parameters are still stripped, but without re-encoding the rest of the URL). Clients must continue to
present the signed URL exactly as Dataverse returned it.

### A signing secret is now required to request signed URLs

The `/api/admin/requestSignedUrl` endpoint now requires a non-empty signing secret
(`dataverse.api.signing-secret`) to be configured. Previously an unset secret silently fell back to
using only the user's API token as the signing key, which is too weak. If the secret is not
configured, the endpoint now returns an error instead of issuing a weakly-signed URL.

**Upgrade note:** installations that use signed URLs through this endpoint (including the
`rdm-integration` connector) must set `dataverse.api.signing-secret`. See the
[Configuration Guide](https://guides.dataverse.org/en/latest/installation/config.html#dataverse-api-signing-secret).
Treat the value like a password.
