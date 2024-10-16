The following API have been added:

/api/users/{identifier}/allowedcollections/{permission}

This API lists the dataverses/collections that the user has access to via the permission passed.
By passing "any" as the permission the list will return all dataverse/collections that the user can access regardless of which permission is used.
This API can be executed only by the User requesting their own list of accessible collections or by an Administrator.
Valid Permissions are: AddDataverse, AddDataset, ViewUnpublishedDataverse, ViewUnpublishedDataset, DownloadFile, EditDataverse, EditDataset, ManageDataversePermissions,
ManageDatasetPermissions, ManageFilePermissions, PublishDataverse, PublishDataset, DeleteDataverse, DeleteDatasetDraft, and "any" as a wildcard option.
