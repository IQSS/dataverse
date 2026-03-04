## Feature Request: API to support Download Terms of Use and Guestbook

## New Endpoints to download a file or files that required a Guestbook Response: POST
A post to these endpoints with the body containing a JSON Guestbook Response will save the response and return a signed URL to download the file(s)

`/api/access/datafile/{fileId:.+}`
`/api/access/datafiles/{fileIds}`
`/api/access/dataset/{id}`
`/api/access/dataset/{id}/versions/{versionId}`

A post to these endpoints with the body containing a JSON Guestbook Response will save the response before continuing the download.
No signed URL option exists.
`/api/access/datafiles`
`/api/access/datafile/bundle/{fileId}` POST returns BundleDownloadInstance after processing Guestbook Responses from body.

## New CRUD Endpoints for Guestbook:
Create a Guestbook: POST `/api/guestbooks/{dataverseIdentifier}`
Get a Guestbook: GET `/api/guestbooks/{id}`
Get a list of Guestbooks linked to a Dataverse Collection: GET `/api/guestbooks/{dataverseIdentifier}/list`
Enable/Disable a Guestbook: PUT `/api/guestbooks/{dataverseIdentifier}/{id}/enabled` Body: `true` or `false`
Note: There is no Update or Delete at this time. You can disable a Guestbook and create a new one.

## For Guestbook At Request:
When JVM setting -Ddataverse.files.guestbook-at-request=true is used a request for access may require a Guestbook Response.
PUT `/api/access/datafile/{id}/requestAccess` will now take a JSON Guestbook Response in the body.
