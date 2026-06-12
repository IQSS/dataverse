## Feature - Manage Guestbook

This feature adds 2 new APIs to help manage Guestbooks and Guestbook Responses.


This API allows the user to make edits to an existing Guestbook, including adding and removing Custom Guestbook Questions.

`curl -PUT -H  "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/guestbooks/{ID}" -d "$JSON"`

This API allows the user to retrieve Guestbook Responses for a specific Guestbook within a Collection. Optional pagination parameters can be added to limit the number of results, as this can get very large.

`curl -H  "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/guestbooks/$ID/responses?limit10&offset=0"`
