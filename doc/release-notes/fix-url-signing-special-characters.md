### Signed URLs work again for URLs with special characters

Requesting a signed URL (e.g. via `/api/admin/requestSignedUrl`, used by external tools, the Globus
integration and third-party integrations such as the `rdm-integration` connector) was broken in 6.10
for URLs whose query contained special characters — most notably persistent IDs such as
`doi:10.5072/FK2/ABC` (which contain `:` and `/`), as well as spaces, percent-encoded values and
non-ASCII characters. In 6.10 the signing step began re-encoding/normalizing the URL (for example
percent-encoding `:` and `/`) before computing the signature, while the request is validated against
the URL the caller actually presents back. The re-encoded signature no longer matched, so validation
failed with 401 "Bad signed URL" authentication errors.

Signing no longer alters the URL at all: it is signed exactly as provided, character for character.
As part of this, a URL to be signed must not already contain any of the reserved query parameters
`until`, `user`, `method`, `token` (added by the signing itself), `key` or `signed`:

- `/api/admin/requestSignedUrl` now returns a 400 (Bad Request) naming the offending parameter when
  the supplied `url` contains one of them. In 6.10 such parameters were silently stripped, so the
  caller received a signature for a different URL than the one submitted; before 6.10 the parameter
  was signed into the URL, with undefined results at validation time.
- The `?signed=true` guestbook-response download flow is unaffected: the reserved parameters that
  legitimately appear in such a request (`signed` itself, `key` when query-parameter API token
  authentication is used, and the four signing parameters when the request was authenticated with an
  existing signed URL) are removed from the request URL before the new signed URL is created, as in
  6.10.

**This restores the URL-signing behavior used before 6.10, so it is compatible with older versions
and with existing integrations.** Clients and connectors that build or consume signed URLs the way
they did before 6.10 keep working unchanged, signatures are computed the same way as before the
regression, and URLs containing special characters validate again. No client-side changes are
required.

Validation has also been made encoding-agnostic. The signature is now checked against the URL
exactly as presented on the wire first, so the simple contract just works: submit the `url` to
`/api/admin/requestSignedUrl` in exactly the form you will use it - percent-encoded or not - and use
the returned signed URL verbatim. URLs signed in their URL-decoded form and later presented as a
percent-encoded variant also continue to validate (the pre-6.10 behavior, kept for compatibility
with existing clients and with proxies or HTTP libraries that re-encode characters in flight).

### A signing secret is now required for signed URLs

Separately from the fix above, Dataverse no longer falls back to a weak signing key when
`dataverse.api.signing-secret` is unset. Previously, with no secret configured, signed URLs were
signed using only the user's API token (or, for a guest, a value derived from the public URL), which
is too weak to be a signing key. A non-empty `dataverse.api.signing-secret` is now required wherever
URLs are signed with a key based on a user's API token:

- The endpoints that issue a signed URL on request - `/api/admin/requestSignedUrl` and the `POST`
  guestbook-response download endpoints under `/api/access` (`datafile/{id}`, `datafiles/{ids}`,
  `dataset/{id}` and `dataset/{id}/versions/{versionId}`) - return an error instead of issuing a
  weakly-signed URL.
- External tool launches send their callback unsigned (with a warning logged) rather than weakly
  signed. Such an unsigned callback only allows anonymous access to public data; it cannot be used to
  access draft datasets or restricted files.
- Globus transfers are effectively disabled without a signing secret: the callbacks the
  dataverse-globus app relies on require an authenticated, signed request, so Globus upload (and
  download of restricted or unpublished files) fails with an authorization error. Installations that
  use Globus must set `dataverse.api.signing-secret`.
- The permissions-history CSV download links on the permission management pages are not offered
  (a warning is logged).

Remote and Globus overlay stores are unaffected: they sign with their own per-store secret key, not
`dataverse.api.signing-secret`.

**Upgrade note:** installations that rely on signed URLs - including the `rdm-integration` connector,
signed guestbook-response downloads, and external tools or Globus transfers that use signed callbacks -
must set `dataverse.api.signing-secret`. See the
[Configuration Guide](https://guides.dataverse.org/en/latest/installation/config.html#dataverse-api-signing-secret).
Treat the value like a password. Because the signing secret is part of the signing key, setting (or
later changing) it invalidates previously issued signed URLs: any existing signed URLs that have not
yet expired will stop working, and clients/integrations will need to request new ones.
