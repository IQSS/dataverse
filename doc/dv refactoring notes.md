# Changelog
_Make sure you undeploy and clean-build before attempting to run this version. Oh, and drop the database while you're at it._

## Changes - up to Sep 10

* LocalAuthenticationProvider &rarr; BuiltinAuthenticationProvider: as "local" sounds more like an IDP service running on the same machine, not necessarily in-app one.
* AuthenticationManager is now a `@Singleton` service bean. This is because the BuiltinServiceProvider needs dependency injections (such as JPA). So we need the application to start before we can register etc.
* Changed the AuthenticationProvider interface. Now includes more realistic methods.
* AuthenticationProvider has two subclasses, one for providers that can be used in-app, and one for those requiring external site visit (e.g. shib).
* Added a "silly" echo provider, mainly for testing purposes.
* Renamed ApiKey to ApiToken, as per @todo.
* Identifiers: Users moved from `u:` to `@`, and groups from `g:` to `&`. Pre-defined role assignees (only Guest, at this point) start with `:`. 50% less characters, 245% more Internet (I think).
* `AuthenticatedServiceBean` replaces most of `UserServiceBean` functionality.
* Api keys now work. On the down side, the setup scripts are more complex, and require jq to be installed.
* For convenience, use setup-all.sh to set up the system and it's test config (Pete, Uma, basic dv structure).

## Changes
* Settings and Settings service bean are now used.
* Introduces `api/s/` for secure API (`/usr/sbin` inspired). We will later add a filter to only allow localhost/whitelist to use it.
    - `api/s/settings/XXXX` used to CRUD settings.
* User signup governed by settings `:AllowSignUp` and `:SignUpUrl`
* Setup script updated to allow signup locally.