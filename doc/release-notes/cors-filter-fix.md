## CORS Filter Fix

Fixed an inconsistency where the `CorsFilter` was not always being invoked when accessing `/api/...` endpoints, preventing these endpoints from being used from webapps even when CORS was properly configured.
