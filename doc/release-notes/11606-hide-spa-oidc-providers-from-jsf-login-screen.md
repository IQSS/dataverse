This release will allow for SPA created OIDC Providers to be hidden from JSF login screens. By setting the ``enabled`` attribute either in the config file ``DATAVERSE_AUTH_OIDC_ENABLED: "0"`` or in the Json of the api call:
POST api/admin/authenticationProviders

{
"id": "oidc1",
"factoryAlias": "oidc",
"title": "Open ID Connect SPA",
"subtitle": "SPA OIDC Provider",
"factoryData": "type: oidc | issuer: http://keycloak.mydomain.com:8090/realms/test | clientId: test | clientSecret: 94XHrfNRwXsjqTqApRrwWmhDLDHpIYV8",
```"enabled": false```
}

Calling GET api/admin/authenticationProviders will return all providers allowing SPA to display even the ones with enabled = false
