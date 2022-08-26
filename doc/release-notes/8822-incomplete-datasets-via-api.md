### Creating datasets with incomplete metadata through API

The create dataset API call (POST to /api/dataverses/#dataverseId/datasets) is extended with the "doNotValidate" parameter. However, in order to be able to create a dataset with invalid metadata, the solr configuration must be updated first with the new "schema.xml" file (do not forget to run the metadata fields update script when you use custom metadata). Reindexing is optional, but recommended. Also, even when this feature is not used, it is recommended to update the solar configuration and reindex the metadata. Finally, this new feature can be activated (after updating the solr configuration) with the ":AllowInvalidMetadataThroughAPI" setting.

You can also enable a valid/invalid metadata filter in the "My Data" page using the ":ShowValidityFilter" setting. By default, this filter is not shown. When you wish to use this filter, you must reindex the datasets first, otherwise datasets with valid metadata will not be shown in the results.

It is not possible to publish datasets with incomplete or invalid metadata. By default, you also cannot send such datasets for review. If you wish to enable sending for review of datasets with invalid metadata, turn on the ":CanReviewInvalid" setting.
