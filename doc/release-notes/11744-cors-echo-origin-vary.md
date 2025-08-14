# 11744: CORS header handling fixes (echo single Origin, add Vary: Origin, multi-origin allow, sanitization)

This branch adjusts the CORS filter so browser clients work correctly when multiple origins are allowed.

## What changed
- Access-Control-Allow-Origin (ACAO) now echoes the single request `Origin` when it matches an allowlist from `dataverse.cors.origin`.
- `Vary: Origin` is added when echoing a specific origin to keep caches correct across different origins.
- Comma-separated origin lists are supported; surrounding quotes in CSV configs are stripped.
- Sanitization is applied to CORS header lists (methods/allow/expose) to avoid quoted values that can break preflight checks.
- Deprecated DB fallback for enabling CORS is removed; CORS is considered enabled only when `dataverse.cors.origin` is set as a JVM options/Microprofile setting.

## Upgrade / run notes (non-SQL)
To keep CORS working after pulling this branch:

1) Configure origins as JVM options/Microprofile settings (no quotes):
- Single origin:
  - `dataverse.cors.origin=https://example.org`
- Multiple origins (comma-separated):
  - `dataverse.cors.origin=https://libis.github.io,https://gdcc.github.io`
- Wildcard:
  - `dataverse.cors.origin=*`
  - Note: Browsers reject `*` when credentialed requests are used (cookies/Authorization headers). Prefer explicit origins for those cases.

2) Optional headers/methods lists (unquoted, comma-separated CSV):
- `dataverse.cors.methods`
- `dataverse.cors.headers.allow`
- `dataverse.cors.headers.expose`

Avoid surrounding values with quotes (e.g., do not use `"Accept, Content-Type"`). Quotes will be stripped but may cause confusion.

3) If you previously relied on the database setting to enable CORS (deprecated `AllowCors`), set `dataverse.cors.origin` instead. The DB fallback is no longer used.

4) Reverse proxies/caches: `Vary: Origin` is now emitted. Ensure your proxy does not drop this header.

## Verify
Preflight (replace DV_URL with your base URL):

```bash
curl -i -X OPTIONS \
  -H "Origin: https://libis.github.io" \
  -H "Access-Control-Request-Method: GET" \
  "${DV_URL}/api/info/version"
```

Expected:
- `Access-Control-Allow-Origin: https://libis.github.io`
- `Vary: Origin` present

Actual request:

```bash
curl -i \
  -H "Origin: https://libis.github.io" \
  "${DV_URL}/api/info/version"
```

Expected:
- Same ACAO echo as above

## Backward compatibility
- Instances relying on the deprecated DB-based CORS enablement must set `dataverse.cors.origin` to keep CORS enabled.
- Quoted CORS configuration values may behave differently; remove quotes going forward.
