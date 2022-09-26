## Adding new static search facet: Metadata Types
A new static search facet has been added to the search side panel. This new facet is called "Metadata Types" and is driven from metadata blocks. When a metadata field value is inserted into a dataset, an entry for the metadata block it belongs to is added to this new facet.

This new facet needs to be configured for it to appear on the search side panel. The configuration assigns to a dataverse what metadata blocks to show. The configuration is inherited by child dataverses.

To configure the new facet, use the Metadata Block Facet API: <https://guides.dataverse.org/en/latest/api/native-api.html#set-metadata-block-facet-for-a-dataverse-collection>