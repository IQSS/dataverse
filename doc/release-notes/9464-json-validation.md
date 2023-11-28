Functionality has been added to help validate dataset JSON prior to dataset creation. There are two new API endpoints in this release. The first takes in a collection alias and returns a custom dataset schema based on the required fields of the collection. The second takes in a collection alias and a dataset JSON file and does an automated validation of the JSON file against the custom schema for the collection. (Issue #9464 and #9465)

For documentation see the API changelog: http://preview.guides.gdcc.io/en/develop/api/changelog.html
