A new feature flag, `dataverse.feature.api-session-auth-hardening`, adds CSRF protection for API requests authenticated via session cookie (JSESSIONID) by validating the `Origin` and `Referer` headers of the request against `dataverse.siteUrl`.

Installations that enable `dataverse.feature.api-session-auth` should enable this flag as well. See [the guides](https://guides.dataverse.org/en/6.12/installation/config.html#dataverse-feature-api-session-auth-hardening) for details.
