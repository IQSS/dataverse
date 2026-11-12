### Optionally reduce database updates of the "last API use time"

Dataverse updates an authenticated user's "last API use time" after every authenticated API call. For clients that make frequent API requests, this means an UPDATE on the `authenticateduser` table after each one.

A new feature flag, `dataverse.feature.delay-lastapiuse-update`, makes Dataverse update the timestamp only if the recorded value is older than a configurable interval. The interval is set in seconds with the new setting `dataverse.api.last-api-use-interval` and defaults to 3600 (one hour). With the flag enabled, the "last API use time" shown for a user may be up to that interval out of date.

See also #12809.

## New Settings

- dataverse.feature.delay-lastapiuse-update
- dataverse.api.last-api-use-interval
