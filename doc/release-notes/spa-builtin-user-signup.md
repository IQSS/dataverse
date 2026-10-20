### Sign Up via API for Builtin Users

New users can now create their own builtin account through a new API endpoint, `POST /api/users/signup`, which is used by the modern Single Page Application (SPA) sign-up form. The endpoint applies the same validation as the "Sign Up" page, honors the `:AllowSignUp` setting, and is rate-limited per client IP address.

New users can also connect their ORCID iD while signing up. When an ORCID OAuth2 provider is configured (it can be registered with `"enabled": false`), the new `GET /api/users/signup/orcid/authorize` and `POST /api/users/signup/orcid` endpoints authenticate the user's ORCID iD, and the sign-up request saves it to the new account. The allowed redirect URLs are set with `dataverse.api.signup.orcid-redirect-uris`.

The JSON representation of a user, for example from `GET /api/users/:me`, now includes `authenticatedOrcid` when the user has linked an ORCID iD.

The endpoints are disabled by default. To enable it, set the `dataverse.feature.api-builtin-user-signup` feature flag. The rate limit can be changed with `dataverse.api.signup.rate-limit-per-hour` (default 10 requests per hour per IP address).

See the [API Guide](https://guides.dataverse.org/en/latest/api/native-api.html#sign-up-builtin-user) for details.

## New Settings

- dataverse.feature.api-builtin-user-signup
- dataverse.api.signup.rate-limit-per-hour
- dataverse.api.signup.orcid-redirect-uris
