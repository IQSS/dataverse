## BUG Fix

Call to API /api/mydata/retrieve was using JSF session data to get the user (if logged in on another browser tab). This caused the other user's data to be returned.
Fixed to use the API token for SPA and session data for JSF

