### API_BEARER_AUTH_USE_BUILTIN_USER_ON_ID_MATCH feature flag

Introduced a new feature flag, ``API_BEARER_AUTH_USE_BUILTIN_USER_ON_ID_MATCH``, which allows the use of a built-in user
account when an identity match is found during OIDC API bearer token authentication.

This feature enables automatic association of an incoming IdP identity with an existing built-in user account, bypassing
the need for additional user registration steps.

### Keycloak SPI for Built-In users

A Keycloak SPI, ``builtin-users-spi``, has been implemented that allows the use of Keycloak on instances with built-in
accounts for OIDC
authentication, enabling the use of the SPA on those instances.

Looking ahead, this authenticator SPI could also support mapping Shibboleth users coming in through Keycloak to existing
Shib users without changing the provider in the Dataverse database. However, this would require changes to the storage
provider to support more than just built-in users.

The SPI code is available in the Dataverse code repository.
