## Database Settings Cleanup

With this release, we remove some legacy specialties around Database Settings and provide better Admin API endpoints for them.

Most important changes:

1. Setting `BuiltinUsers.KEY` was renamed to `:BuiltinUsersKey`, aligned with our general naming pattern for options.
2. Setting `WorkflowsAdmin#IP_WHITELIST_KEY` was renamed to `:WorkflowsAdminIpWhitelist`, aligned with our general naming pattern for options.
3. Setting `:TabularIngestSizeLimit` no longer uses suffixes for formats and becomes a JSON-based setting instead.
4. If set, all three settings will be migrated to their new form automatically for you (Flyway migration).
5. You can no longer (accidentally) create or use arbitrary setting names or languages.
   All Admin API endpoints for settings now validate setting names and languages for existence and compliance.

As an administrator of a Dataverse instance, you can now make use of enhanced Bulk Operations on the Settings Admin API:

1. Retrieving all settings as JSON via `GET /api/admin/settings` supports localized options now, too.
2. You can replace all existing settings in an idempotent way sending JSON to `PUT /api/admin/settings`.
   This will create, update and remove settings as necessary in one atomic operation.  
   The new endpoint is especially useful to admins using GitOps or other automations.
   It allows control over all Database Settings from a single source without risking an undefined state.

Note: Despite the validation of setting names and languages, the content of any database setting is still not being validated when using the Settings Admin API!

### Updated Database Settings

The following database settings are were added to the official list within the code (to remain valid with the settings cleanup mentioned above):

- `:BagGeneratorThreads`
- `:BagItHandlerEnabled`
- `:BagItLocalPath`
- `:BagValidatorJobPoolSize`
- `:BagValidatorJobWaitInterval`
- `:BagValidatorMaxErrors`
- `:BuiltinUsersKey` - formerly `BuiltinUsers.KEY`
- `:CreateDataFilesMaxErrorsToDisplay`
- `:DRSArchiverConfig` - a Harvard-specific setting
- `:DuraCloudContext`
- `:DuraCloudHost`
- `:DuraCloudPort`
- `:FileCategories`
- `:GoogleCloudBucket`
- `:GoogleCloudProject`
- `:LDNAnnounceRequiredFields`
- `:LDNTarget`
- `:WorkflowsAdminIpWhitelist` - formerly `WorkflowsAdmin#IP_WHITELIST_KEY`
- `:PrePublishDatasetWorkflowId` - formerly `WorkflowServiceBean.WorkflowId:PrePublishDataset`
- `:PostPublishDatasetWorkflowId` - formerly `WorkflowServiceBean.WorkflowId:PostPublishDataset`

### Important Considerations During Upgrade Of Your Installation

1. Running a customized fork? Make sure to add any custom settings to the SettingsServiceBean.Key enum before deploying!
2. Any database settings not contained in the `SettingServiceBean.Key` will be removed from your database during each deployment cycle.
3. As always when upgrading, make sure to backup your database beforehand!
   You can also use the existing API endpoint `/api/admin/settings` to retrieve all settings as JSONish data for a quick backup before upgrading.

