Search API (/api/search) response will now include new fields for the different entities.

For Dataverse:

- "affiliation" 
- "parentDataverseName"
- "parentDataverseIdentifier"
- "image_url" (optional)

```javascript
"items": [
    {
        "name": "Darwin's Finches",
        ...
        "affiliation": "Dataverse.org",
        "parentDataverseName": "Root",
        "parentDataverseIdentifier": "root",
        "image_url":"data:image/png;base64,iVBORw0..."
(etc, etc)
```

For DataFile:

- "releaseOrCreateDate"
- "image_url" (optional)

```javascript
"items": [
    {
        "name": "test.txt",
        ...
        "releaseOrCreateDate": "2016-05-10T12:53:39Z",
        "image_url":"data:image/png;base64,iVBORw0..."
(etc, etc)
```

For Dataset:

- "image_url" (optional)

```javascript
"items": [
    {
        ...
        "image_url": "http://localhost:8080/api/datasets/2/logo"
        ...
(etc, etc)
```

The image_url field was already part of the SolrSearchResult JSON (and incorrectly appeared in Search API documentation), but it wasn’t returned by the API because it was appended only after the Solr query was executed in the SearchIncludeFragment of JSF. Now, the field is set in SearchServiceBean, ensuring it is always returned by the API when an image is available.

The schema.xml file for Solr has been updated to include a new field called dvParentAlias for supporting the new response field "parentDataverseIdentifier". So for the next Dataverse released version, a Solr reindex will be necessary to apply the new schema.xml version.
