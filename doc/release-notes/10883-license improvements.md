- License metadata enhancements (#10883):
  - Added new fields to licenses: rightsIdentifier, rightsIdentifierScheme, schemeUri, languageCode
  - Updated DataCite metadata export to include rightsIdentifier, rightsIdentifierScheme, and schemeUri consistent with the DataCite 4.5 schema and examples
  - Enhanced metadata exports to include all new license fields
  - Existing licenses from the example set included with Dataverse will be automatically updated with new fields
  - Existing API calls support the new optional fields
  
  Setup: For existing published datasets, the additional license metadata will not be available from DataCite or in metadata exports until the dataset is republished or
  - the /api/admin/metadata/{id}/reExportDataset is run for the dataset
  - the api/datasets/{id}/modifyRegistrationMetadata API is run for the dataset,
   or the global version of these api calls (/api/admin/metadata/reExportAll, /api/datasets/modifyRegistrationPIDMetadataAll) are used. 
  