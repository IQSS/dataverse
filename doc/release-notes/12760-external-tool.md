## BUG ##
API /api/files/{FILE_ID}/externalTool/{TOOL_ID}/toolUrl was not allowing a user to download the file being previewed after entering the guestbook response.
The api now includes the query parameter ?guestbookresponseid=# to be included in the callback to allow for guestbook response verification.

