### Remap oai_dc export and harvesting format fields: dc:type and dc:date

The `oai_dc` export and harvesting format has had the following fields remapped:

- dc:type was mapped to the field "Kind of Data". Now it is hard-coded to the word "Dataset".
- dc:date was mapped to the field "Production Date" when available and otherwise to "Publication Date". Now it is mapped the field "Publication Date" or the field used for the citation date, if set (see [Set Citation Date Field Type for a Dataset](https://guides.dataverse.org/en/6.3/api/native-api.html#set-citation-date-field-type-for-a-dataset)).

In order for these changes to be reflected in existing datasets, a [reexport all](https://guides.dataverse.org/en/6.3/admin/metadataexport.html#batch-exports-through-the-api) should be run.

For more information, please see #8129 and #10737.

### Backward incompatible changes 

See the "Remap oai_dc export" section above.

### Upgrade instructions

In order for changes to the `oai_dc` metadata export format to be reflected in existing datasets, a [reexport all](https://guides.dataverse.org/en/6.3/admin/metadataexport.html#batch-exports-through-the-api) should be run.
