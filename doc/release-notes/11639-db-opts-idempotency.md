## Database Settings Cleanup

With this release, we remove some legacy specialties around Database Settings and provide better Admin API endpoints for them.

Most important changes:

1. Setting `BuiltinUsers.KEY` was renamed to `:BuiltinUsersKey`, aligned with our general naming pattern for options.
2. Setting `:TabularIngestSizeLimit` no longer uses suffixes for formats and becomes a JSON-based setting instead.
3. If set, both settings will be migrated to their new form automatically for you (Flyway migration).
4. You can no longer (accidentally) create or use arbitrary setting names or languages.
   All Admin API endpoints for settings now validate setting names and languages for existence and compliance.

As an administrator of a Dataverse instance, you can now make use of enhanced Bulk Operations on the Settings Admin API:

1. Retrieving all settings as JSON via `GET /api/admin/settings` supports localized options now, too.
2. You can replace all existing settings in an idempotent way sending JSON to `PUT /api/admin/settings`.
   This will create, update and remove settings as necessary in one atomic operation.  
   The new endpoint is especially useful to admins using GitOps or other automations.
   It allows control over all Database Settings from a single source without risking an undefined state.

Note: Despite the validation of setting names and languages, the content of any database setting is still not being validated when using the Settings Admin API!
