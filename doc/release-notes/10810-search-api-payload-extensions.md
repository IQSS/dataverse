Search API (/api/search) response will now include new fields for Dataverse and DataFile entities.

For Dataverse:

- "affiliation" 
- "parentDataverseName"
- "parentDataverseIdentifier"

```javascript
"items": [
    {
        "name": "Darwin's Finches",
        ...
        "affiliation": "Dataverse.org",
        "parentDataverseName": "Root",
        "parentDataverseIdentifier": "root"
(etc, etc)
```

For DataFile:

- "releaseOrCreateDate"

```javascript
"items": [
    {
        "name": "test.txt",
        ...
        "releaseOrCreateDate": "2016-05-10T12:53:39Z"
(etc, etc)
```

The schema.xml file for Solr has been updated to include a new field called dvParentAlias for supporting the new response field "parentDataverseIdentifier". So for the next Dataverse released version, a Solr reindex will be necessary to apply the new schema.xml version.
