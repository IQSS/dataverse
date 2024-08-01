### Remap oai_dc export and harvesting format fields: dc:type, dc:date, and dc:rights

The `oai_dc` export and harvesting format has had the following fields remapped:

- dc:type was mapped to the field "Kind of Data". Now it is hard-coded to the word "Dataset".
- dc:date was mapped to the field "Production Date" when available and otherwise to "Publication Date". Now it is mapped only to the field "Publication Date".
- dc:rights was not mapped to anything. Now it is mapped (when available) to terms of use, restrictions, and license.

As these are backward incompatible changes, they have been noted in the [API changelog](https://guides.dataverse.org/en/latest/api/changelog.html).

For more information, please see issue #8129.
