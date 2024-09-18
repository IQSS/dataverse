Search API (/api/search) response will now include new image_url format for the Datafile and Dataverse logo.
Note to release note writer: this supersedes the release note 10810-search-api-payload-extensions.md

For Dataverse:

- "image_url" (optional)

```javascript
"items": [
    {
        "name": "Darwin's Finches",
        ...
        "image_url":"/api/access/dvCardImage/{identifier}"
(etc, etc)
```

For DataFile:

- "image_url" (optional)

```javascript
"items": [
    {
        "name": "test.txt",
        ...
        "image_url":"/api/access/datafile/{identifier}?imageThumb=true"
(etc, etc)
```
