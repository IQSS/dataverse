## BUG ##
API /api/files/{FILE_ID}/externalTool/{TOOL_ID}/toolUrl was not allowing a user to download the file being previewed after entering the guestbook response.
The api now includes the query parameter ?guestbookresponseid=# to be included in the callback to allow for guestbook response verification.
New JVM setting ("dataverse.api.signing.external-tool-callback-timeout-in-minutes") to override the expiration of a signed url (for external tool callbacks) from the default 10 minutes.

