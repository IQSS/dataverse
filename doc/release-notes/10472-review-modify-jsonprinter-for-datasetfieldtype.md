### Json Printer Bug fix

DatasetFieldTypes in MetadataBlock response that are also a child of another DatasetFieldType were being returned twice. The child DatasetFieldType was included in the "fields" object as well as in the "childFields" of it's parent DatasetFieldType. This fix suppresses the standalone object so only one instance of the DatasetFieldType is returned (in the "childFields" of its parent).
This fix changes the Json output of the API `/api/dataverses/{dataverseAlias}/metadatablocks`

## Backward Incompatible Changes

The Json response of API call `/api/dataverses/{dataverseAlias}/metadatablocks` will no longer include the DatasetFieldTypes in "fields" if they are children of another DatasetFieldType. The child DatasetFieldType will only be included in the "childFields" of it's parent DatasetFieldType.
