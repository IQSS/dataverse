### API_BEARER_AUTH_USE_BUILTIN_USER_ON_ID_MATCH feature flag

Introduced a new feature flag, ``API_BEARER_AUTH_USE_BUILTIN_USER_ON_ID_MATCH``, which allows the use of a built-in user
account when an identity match is found during OIDC API bearer token authentication.

This feature enables automatic association of an incoming IdP identity with an existing built-in user account, bypassing
the need for additional user registration steps.

### New bultin-users API endpoint

``/builtin-users/{username}/canLoginWithGivenCredentials``

Validates the provided credentials to determine if the user can log in with them.
