## OpenID Connect Authentication Provider Improvements

### Using MicroProfile Config For Provisioning

With this release it is possible to provision a single OIDC-based authentication provider
by using MicroProfile Config instead of or in addition to the classic Admin API provisioning.

If you are using an external OIDC provider component as identity management system and/or broker
to other authentication providers such as Google, eduGain SAML and so on, this might make your
life easier during instance setups and reconfiguration. You no longer need to generate the
necessary JSON file.

### Adding PKCE Support

Some OIDC providers require using PKCE as additional security layer. As of this version, you can enable
support for this on any OIDC provider you configure. (Note that OAuth2 providers have not been upgraded.)

## Improved Testing

With this release, we add a new type of testing to Dataverse: integration tests which are no end-to-end tests
like our API tests. Starting with OIDC authentication support, we test regularly on CI for working condition
of both OIDC login options in UI and API.

The testing and development Keycloak realm has been updated with more users and compatibility with Keycloak 21.

The support for setting JVM options during testing has been improved for developers. You now may add the
`@JvmSetting` annotation to classes (also inner classes) and reference factory methods for values. This improvement is
also paving the way to enable manipulating JVM options during end-to-end tests on remote ends.
