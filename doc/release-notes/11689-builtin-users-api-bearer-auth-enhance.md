## Security improvements for `api-bearer-auth-use-builtin-user-on-id-match`

We’ve strengthened the security of the `api-bearer-auth-use-builtin-user-on-id-match` feature flag. It will now only work when the provided bearer token includes an `idp` claim that matches the Keycloak Service Provider identifier.

By enforcing this check, the risk of impersonation from other identity providers is significantly reduced, since they would need to be explicitly configured with this specific, non-standard identifier.

See:
- [#11622 (comment)](https://github.com/IQSS/dataverse/pull/11622#discussion_r2216017175)
- [#11689](https://github.com/IQSS/dataverse/issues/11689)  
