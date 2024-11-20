Extends the OIDC API auth mechanism (available through feature flag ``api-bearer-auth``) to properly handle cases
where ``BearerTokenAuthMechanism`` successfully validates the token but cannot identify any Dataverse user because there
is no account associated with the token.

To register a new user who has authenticated via an OIDC provider, a new endpoint has been
implemented (``/users/register``). A feature flag called ``api-bearer-auth-json-claims`` has been implemented to allow
sending missing user claims in the request JSON. This is useful when the identity provider does not supply the necessary
claims. However, this flag will only be considered if the ``api-bearer-auth`` feature flag is enabled. If the latter is
not enabled, the ``api-bearer-auth-json-claims`` flag will be ignored.
