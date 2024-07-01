In Dataverse 6.0 Payara was updated, which caused the url `/openapi` to stop working:

- https://github.com/IQSS/dataverse/issues/9981
- https://github.com/payara/Payara/issues/6369

When it worked in Dataverse 5.x, the `/openapi` output was generated automatically by Payara, but in this release we have switched to OpenAPI output produced by the [SmallRye OpenAPI plugin](https://github.com/smallrye/smallrye-open-api/tree/main/tools/maven-plugin). This gives us finer control over the output.

For more information, see the section on  [OpenAPI](https://dataverse-guide--10328.org.readthedocs.build/en/10328/api/getting-started.html#openapi) in the API Guide.
