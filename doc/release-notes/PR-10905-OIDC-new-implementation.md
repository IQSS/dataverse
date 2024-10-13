New OpenID Connect implementation including new log in scenarios (see [the guides](https://dataverse-guide--10905.org.readthedocs.build/en/10905/installation/oidc.html#choosing-provisioned-providers-at-log-in)) for the current JSF frontend, the new Single Page Application (SPA) frontend, and a generic API usage. The API scenario using Bearer Token authorization is illustrated with a Python script that can be found in the `doc/sphinx-guides/_static/api/bearer-token-example` directory. This Python script prompts you to log in to the Keycloak in a new browser window using selenium. You can run that script with the following commands:

```shell
    cd doc/sphinx-guides/_static/api/bearer-token-example
    ./run.sh
```

This script is safe for production use, as it does not require you to know the client secret or the user credentials. Therefore, you can safely distribute it as a part of your own Python script that lets users run some custom tasks.

The following settings become deprecated with this change and can be removed from the configuration:
- `dataverse.auth.oidc.pkce.enabled`
- `dataverse.auth.oidc.pkce.method`
- `dataverse.auth.oidc.pkce.max-cache-size`
- `dataverse.auth.oidc.pkce.max-cache-age`

The following settings new:
- `dataverse.auth.oidc.issuer-identifier`
- `dataverse.auth.oidc.issuer-identifier-field`
- `dataverse.auth.oidc.subject-identifier-field`

Also, the bearer token authentication is now always enabled. Therefore, the `dataverse.feature.api-bearer-auth` feature flag is no longer used and can be removed from the configuration as well.

The new implementation relies now on the builtin OIDC support in our application server (Payara). With this change the Nimbus SDK is no longer used and is removed from the dependencies.
