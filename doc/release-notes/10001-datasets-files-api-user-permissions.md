- New query parameter `includeDeaccessioned` added to the getVersion endpoint (/api/datasets/{id}/versions/{versionId}) to consider deaccessioned versions when searching for versions.
  

- New endpoint to get user permissions on a dataset (/api/datasets/{id}/userPermissions). In particular, the user permissions that this API call checks, returned as booleans, are the following:

  - Can view the unpublished dataset
  - Can edit the dataset
  - Can publish the dataset
  - Can manage the dataset permissions
  - Can delete the dataset draft


- New permission check "canManageFilePermissions" added to the existing endpoint for getting user permissions on a file (/api/access/datafile/{id}/userPermissions).