Get Dataset API (/api/datasets/) response will now include latestPublishedVersionId in the Json response under data.latestVersion as long as the Dataset was published at least once.

Example:
```javascript
"latestVersion": {
    "id": 83,
    "datasetId": 115,
    "datasetPersistentId": "doi:10.5072/FK2/FEEFFV",
    "storageIdentifier": "local://10.5072/FK2/FEEFFV",
    "versionState": "DRAFT",
    "latestVersionPublishingState": "DRAFT",
    "lastUpdateTime": "2024-08-16T20:04:09Z",
    "createTime": "2024-08-16T20:04:09Z",
    "publicationDate": "2024-08-16",
    "citationDate": "2024-08-16",
    "latestPublishedVersionId": 82,
(etc, etc)
```
