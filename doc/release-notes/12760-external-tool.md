## BUG ##
API /api/files/{FILE_ID}/externalTool/{TOOL_ID}/toolUrl was not allowing a user to download the file being previewed after entering the guestbook response.
The api now allows the guestbookResponse to be passed in the via the JSON Body. The response will be written to the database and the guestbook id will be included in the callback to allow for guestbook response verification.
Also allows for the private url timeouts for download file with guestbook response to be configured via JVM. 
Url timeout defaults to 1 minute configurable by "dataverse.api.guestbook-response-timeout-minutes". 
External tool downloads default to 60 minutes configurable by "dataverse.api.guestbook-response-external-tool-timeout-minutes".
