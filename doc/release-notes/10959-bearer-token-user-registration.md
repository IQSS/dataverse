The OIDC Bearer token API authentication feature (available through a feature flag) has been extended to allow the registration of new users in Dataverse when there is no user account associated with the bearer token. 

Specifically, a new endpoint (users/register) has been implemented, to which the bearer token and new user account information are sent, allowing the identity provider user to be linked to a Dataverse account. 

In this way, the user will be recognized in future requests using the bearer token in the BearerTokenAuthMechanism.
