Functionality has been added to help validate dataset json prior to dataset creation. There are two new API endpoints in this release. The first takes in a Dataverse Collection alias and returns a custom schema based on the required fields of the collection.
The second takes in a Dataverse collection alias and a dataset json file and does an automated validation of the json file against the custom schema for the collection. (Issue 9464 and 9465)

