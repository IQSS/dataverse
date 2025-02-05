### Source Name in harvesting client

A new field `sourceName` has been added to the harvesting client entity.
This will help to customize the metadata source facet for harvested datasets. 
This field is usable from the user interface and the API.
Multiple harvesting client can appear under the same metadata source if you wish.
Note that you must activate the [feature flag](https://guides.dataverse.org/en/latest/installation/config.html#feature-flags) `index-harvested-metadata-source` to beneficiate from this feature.
(See #10217)
