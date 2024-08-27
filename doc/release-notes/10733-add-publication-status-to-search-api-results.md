Search API (/api/search) response will now include publicationStatuses in the Json response as long as the list is not empty

Example:
```javascript
"items": [
    {
        "name": "Darwin's Finches",
        ...
        "publicationStatuses": [
            "Unpublished",
            "Draft"
        ],
(etc, etc)
```
