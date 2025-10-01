# 11744: CORS handling improvements

Modernizes CORS so browser integrations (previewers, external tools, JS clients) work correctly with multiple origins and proper caching.

## Highlights
- Echoes the request origin (`Access-Control-Allow-Origin`) when it matches `dataverse.cors.origin`.
- Adds `Vary: Origin` for per-origin responses (not for wildcard).
- Supports comma‑separated origin list; any `*` in the list = wildcard mode.
- CORS now only enabled when `dataverse.cors.origin` is set (deprecated `:AllowCors` no longer enables it).
- Allows readable spacing in CORS list settings (`dataverse.cors.methods`, `dataverse.cors.headers.allow`, `dataverse.cors.headers.expose`): spaces around commas are ignored; tokens are otherwise unchanged (no quote parsing).
- Docs updated (Installation, Big Data Support, External Tools, File Previews); new tests cover edge cases.

## Admin Action
Set `dataverse.cors.origin` explicitly (required). Use explicit origins (not `*`) for credentialed requests. Ensure proxies keep `Vary: Origin`.

Examples:
```
dataverse.cors.origin=https://example.org
dataverse.cors.origin=https://libis.github.io,https://gdcc.github.io
dataverse.cors.origin=*
```
Optional (unquoted):
```
dataverse.cors.methods=GET, POST, OPTIONS, PUT, DELETE
```

## Compatibility
- Must configure `dataverse.cors.origin`; `:AllowCors` no longer sufficient.
- Any `*` triggers wildcard (no per-origin echo / no Vary header).

## Docs
See updated `dataverse.cors.origin` section and related notes in Big Data Support (S3), External Tools, and File Previews.

