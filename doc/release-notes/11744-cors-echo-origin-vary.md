# 11744: CORS handling improvements

Modernizes CORS so browser integrations (previewers, external tools, JS clients) work correctly with multiple origins and proper caching.

## Highlights

- Echoes the request origin (`Access-Control-Allow-Origin`) when it matches `dataverse.cors.origin`.
- Adds `Vary: Origin` for per-origin responses (not for wildcard).
- Supports comma‑separated origin list; any `*` in the list = wildcard mode.
- CORS now only enabled when `dataverse.cors.origin` is set (removed `:AllowCors` no longer enables it).
- All comma-separated configuration settings (database properties and MicroProfile config) now ignore spaces around commas; tokens remain unchanged (no quote parsing). Examples: `dataverse.cors.methods`, `dataverse.cors.headers.allow`, `dataverse.cors.headers.expose`. See "Comma-separated configuration values" in the Installation Guide.
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

<!-- Maintainer note: The generic behavior for comma-separated settings has been documented centrally under Installation Guide > Configuration > "Comma-separated configuration values". Keep this item here as a cross-reference. -->
