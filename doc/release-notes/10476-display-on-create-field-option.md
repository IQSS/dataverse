New feature: Collection administrators can now configure which metadata fields appear during dataset creation through the `displayOnCreate` property, even when fields are not required. This provides greater control over metadata visibility and can help improve metadata completeness.

- The feature is currently available through the API endpoint `/api/dataverses/{alias}/inputLevels`
- UI implementation will be available in a future release

For more information, see the [API Guide](https://guides.dataverse.org/en/latest/api/native-api.html#update-collection-input-levels) and issues [#10476](https://github.com/IQSS/dataverse/issues/10476) and [#11224](https://github.com/IQSS/dataverse/pull/11224).