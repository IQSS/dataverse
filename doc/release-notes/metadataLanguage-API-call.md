A new API endpoint has been implemented for getting the metadata language of a Dataverse Collection:

`GET /dataverses/{alias}/allowedMetadataLanguages`: Returns the specified metadata language(s) in the collection if any.
`PUT /dataverses/{alias}/allowedMetadataLanguages{metadataLanguage}`: Sets a metadata language in the collection.

For more information, see #11856 and #11856.