### SchemaSpy Generation Is Containerized

SchemaSpy documentation of the database is now generated in GitHub Actions with the official SchemaSpy container image, instead of by the dataverse-ansible role. A new "SchemaSpy" job in the "Dataverse Tests" workflow runs it on a fresh stack from the same build artifact, alongside the JSF tests, and uploads the output as a tarball in an artifact named `schemaspy-<version>`.

See also #12900 and the [SchemaSpy section](https://guides.dataverse.org/en/latest/developers/tips.html#schemaspy) of the Developer Guide.
