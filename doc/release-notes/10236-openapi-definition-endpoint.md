On Dataverse 6.0 Payara was updated and this caused the url `/openapi/` to stop working and caused an exception on the server.

- https://github.com/IQSS/dataverse/issues/9981
- https://github.com/payara/Payara/issues/6369

We incorporated the SmallRye OpenAPI plugin on our POM (https://github.com/smallrye/smallrye-open-api/tree/main/tools/maven-plugin) which will generate files for YAML and JSON formats and deposit them on `edu/harvard/iq/dataverse/openapi/ `these files will be provided by this endpoint depending on the format requested.

The API endpoint was added `/api/info/openapi/{format}` accepting the "json" and "yaml" as formats.