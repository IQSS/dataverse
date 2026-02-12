### Bug Fix

Handles from hdl.handle.net with urls of `/citation` instead of `/dataset.xhtml` were not properly redirecting. This fix adds a lookup for alternate PID so `/citation` endpoint will redirect to `/dataset.xhtml`


