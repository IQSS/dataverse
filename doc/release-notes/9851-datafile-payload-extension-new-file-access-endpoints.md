Implemented the following new endpoints:

- userFileAccessRequested (/api/access/datafile/{id}/userFileAccessRequested): Returns true or false depending on whether or not the calling user has requested access to a particular file. 


- hasBeenDeleted (/api/files/{id}/hasBeenDeleted): Know if a particular file that existed in a previous version of the dataset no longer exists in the latest version.


In addition, the DataFile API payload has been extended to include the following fields:

- tabularData: Boolean field to know if the DataFile is of tabular type


- fileAccessRequest: Boolean field to know if the file access requests are enabled on the Dataset (DataFile owner) 
