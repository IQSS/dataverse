This release adds support for defining storage size quotas for collections. Please see the API guide for details. This is an experimental feature that has not yet been used in production on any real life Dataverse instance, but we are planning to try it out at Harvard/IQSS.
Please note that this release includes a database update (via a Flyway script) that will calculate the storage sizes of all the existing datasets and collections on the first deployment. On a large production database with tens of thousands of datasets this may add a couple of extra minutes to the deployment.

