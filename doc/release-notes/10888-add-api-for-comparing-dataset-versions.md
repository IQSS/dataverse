The following API have been added:

/api/datasets/{persistentId}/versions/{versionId0}/compare/{versionId1}

This API lists the changes between 2 dataset versions. The Json response shows the changes per field within the Metadata block and the Terms Of Access. Also listed are the files that have been added or removed. Files that have been modified will also display the new file data plus the fields that have been modified.

Old and New values are represented by "0" and "1" respectively.
```json
[
   "ModifiedFieldName" = {
      "0" : "old value",
      "1" : "new value"
   }
]
```
