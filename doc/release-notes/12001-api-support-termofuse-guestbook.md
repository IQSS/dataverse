## Feature Request: API to support Download Terms of Use and Guestbook

## New Endpoint to download a file that required a Guestbook response: POST `/api/access/datafile/{id}`
A post to this endpoint with the body containing a JSON Guestbook Response will save the response and return a signed URL to download the file

## New CRUD Endpoints for Guestbook:
Create a Guestbook: POST `/api/guestbooks/{dataverseIdentifier}`
Get a Guestbook: GET `/api/guestbooks/{id}`
Get a list of Guestbooks linked to a Dataverse Collection: GET `/api/guestbooks/{dataverseIdentifier}/list`
Enable/Disable a Guestbook: PUT `/api/guestbooks/{dataverseIdentifier}/{id}/enabled` Body: `true` or `false`
Note: There is no Update or Delete at this time. You can disable a Guestbook and create a new one.

## For Guestbook At Request:
When JVM setting -Ddataverse.files.guestbook-at-request=true is used a request for access may require a Guestbook response.
PUT `/api/access/datafile/{id}/requestAccess` will now take a JSON Guestbook response in the body.
