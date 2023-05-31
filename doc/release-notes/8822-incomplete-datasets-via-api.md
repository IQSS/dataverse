### Creating datasets with incomplete metadata through API

The create dataset API call (POST to /api/dataverses/#dataverseId/datasets) is extended with the "doNotValidate" parameter. However, in order to be able to create a dataset with incomplete metadata, the solr configuration must be updated first with the new "schema.xml" file (do not forget to run the metadata fields update script when you use custom metadata). Reindexing is optional, but recommended. Also, even when this feature is not used, it is recommended to update the solar configuration and reindex the metadata. Finally, this new feature can be activated with the "dataverse.api.allow-incomplete-metadata" JVM option.

You can also enable a valid/incomplete metadata filter in the "My Data" page using the "dataverse.ui.show-validity-filter" JVM option. By default, this filter is not shown. When you wish to use this filter, you must reindex the datasets first, otherwise datasets with valid metadata will not be shown in the results.

It is not possible to publish datasets with incomplete or incomplete metadata. By default, you also cannot send such datasets for review. If you wish to enable sending for review of datasets with incomplete metadata, turn on the "dataverse.ui.allow-review-for-incomplete" JVM option.

In order to customize the wording and add translations to the UI sections extended by this feature, you can edit the "Bundle.properties" file and the localized versions of that file. The property keys used by this feature are:
- incomplete
- valid
- dataset.message.incomplete.warning
- mydataFragment.validity
- dataverses.api.create.dataset.error.mustIncludeAuthorName
