### Metadata Source Facet Can Now Differentiate Between Harvested Sources

A new [feature flag](https://dataverse-guide--11217.org.readthedocs.build/en/11217/installation/config.html#feature-flags) called `index-harvested-metadata-source` has been added that, when enabled, adjusts the "Metadata Source" facet to list name you specify for each service you harvest from. When not enabled, you will see the existing behavior of having all harvested sources under the name "Harvested".

When editing harvesting clients in the user interface, the new field is called "Source Name" ("sourceName" in the [API](https://dataverse-guide--11217.org.readthedocs.build/en/11217/api/native-api.html#create-a-harvesting-client)). To group various harvesting clients under the same name, you can use the same source name.

See also #10217 and #11217.

## New settings

- dataverse.feature.index-harvested-metadata-source

## Upgrade instructions

If you have enabled the `dataverse.feature.index-harvested-metadata-source` feature flag and given some of your harvesting clients a source name, you should reindex to have those source names appear under the "Metadata Source" facet.
