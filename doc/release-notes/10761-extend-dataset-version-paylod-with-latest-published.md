Get Dataset API (/api/datasets/) response will now include latestPublishedVersionId in the Json response under data.latestVersion as long as the Dataset was published at least once.

Example:
```javascript
"latestVersion": {
    "id": 3,
        "datasetId": 5,
        "datasetPersistentId": "doi:10.5072/FK2/J50XYQ",
        "storageIdentifier": "local://10.5072/FK2/J50XYQ",
        "versionState": "DRAFT",
        "latestVersionPublishingState": "DRAFT",
        "lastUpdateTime": "2024-08-16T20:33:02Z",
        "createTime": "2024-08-16T20:33:02Z",
        "publicationDate": "2024-08-16",
        "citationDate": "2024-08-16",
        "latestPublishedVersion": {
            "id": 2,
            "versionNumber": 1,
            "versionMinorNumber": 0
        },
(etc, etc)
```
