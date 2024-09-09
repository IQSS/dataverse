### Remap oai_dc export and harvesting format fields: dc:type and dc:date

The `oai_dc` export and harvesting format has had the following fields remapped:

- dc:type was mapped to the field "Kind of Data". Now it is hard-coded to the word "Dataset".
- dc:date was mapped to the field "Production Date" when available and otherwise to "Publication Date". Now it is mapped the field “Publication Date” or the field used for the citation date, if set (see [Set Citation Date Field Type for a Dataset](https://guides.dataverse.org/en/6.3/api/native-api.html#set-citation-date-field-type-for-a-dataset)).

As these are backward incompatible changes, they have been noted in the [API changelog](https://guides.dataverse.org/en/latest/api/changelog.html).

For more information, please see issue #8129.
