### Metadata Source Facet Can Now Differentiate Between Harvested Sources

The behavior of the feature flag `index-harvested-metadata-source` and the "Metadata Source" facet, which were added and updated, respectively, in [Dataverse 6.3](https://github.com/IQSS/dataverse/releases/tag/v6.3) (through pull requests #10464 and #10651), have been updated. A new field called "Source Name" has been added to harvesting clients.

Before Dataverse 6.3, all harvested content (datasets and files) appeared together under "Harvested" under the "Metadata Source" facet. This is still the behavior of Dataverse out of the box. Since Dataverse 6.3, enabling the `index-harvested-metadata-source` feature flag (and reindexing) resulted in harvested content appearing under the nickname for whatever harvesting client was used to bring in the content. This meant that instead of having all harvested content lumped together under "Harvested", content would appear under "client1", "client2", etc.

Now, as this release, enabling the `index-harvested-metadata-source` feature flag, populating a new field for harvesting clients called "Source Name" ("sourceName" in the [API](https://dataverse-guide--11217.org.readthedocs.build/en/11217/api/native-api.html#create-a-harvesting-client)), and reindexing (see upgrade instructions below), results in the source name appearing under the "Metadata Source" facet rather than the harvesting client nickname. This gives you more control over the name that appears under the "Metadata Source" facet and allows you to group harvested content from various harvesting clients under the same name if you wish (by reusing the same source name).

Previously, `index-harvested-metadata-source` was not documented in the guides, but now you can find information about it under [Feature Flags](https://dataverse-guide--11217.org.readthedocs.build/en/11217/installation/config.html#feature-flags). See also #10217 and #11217.

## Upgrade instructions

If you have enabled the `dataverse.feature.index-harvested-metadata-source` feature flag and given some of your harvesting clients a source name, you should reindex to have those source names appear under the "Metadata Source" facet.
