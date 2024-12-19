Dataverse now supports the option of adding a versionNote before/during publication of a dataset that can be used to indicate why a version was created and/or how it differs from the prior version. Whether this feature is enabled is controlled by a flag. Version notes are shown in the user interface (dataset page version table), indexed, available via the API, and have been added to the JSON, DDI, DataCite, and OAI-ORE exports.

With the addition of this feature, work has been done to clean-up and rename fields that have been used for specifying the reason for deaccessioning a dataset and providing an optional link to a non-Dataverse location where the dataset still can be found. The former was listed in some JSON-based API calls and exports as "versionNote" and is now "deaccessionNote", while the latter was referred to as "archiveNote" and is now "deacccessionLink". These result in incompatibilities in the UI related to deaccessioned datasets.

Further, some database consolidation has been done to combine the deaccessionlink and archivenote fields which appear to have both been used for the same purpose (the deaccessionlink db field is older and was not displayed in the current UI. Going forward, only the deaccessionlink column exists.

New Feature Flags:

VERSION_NOTE - false by default.

Update of the solr schema (using the standard instructions) is needed.

