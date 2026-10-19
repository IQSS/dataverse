### Sign Up via API for Builtin Users

New users can now create their own builtin account through a new API endpoint, `POST /api/users/signup`, which is used by the modern Single Page Application (SPA) sign-up form. The endpoint applies the same validation as the "Sign Up" page, honors the `:AllowSignUp` setting, and is rate-limited per client IP address.

The endpoint is disabled by default. To enable it, set the `dataverse.feature.api-builtin-user-signup` feature flag. The rate limit can be changed with `dataverse.api.signup.rate-limit-per-hour` (default 10 requests per hour per IP address).

See the [API Guide](https://guides.dataverse.org/en/latest/api/native-api.html#sign-up-builtin-user) for details.

## New Settings

- dataverse.feature.api-builtin-user-signup
- dataverse.api.signup.rate-limit-per-hour
