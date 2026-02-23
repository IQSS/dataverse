## Croissant Support Is Now Built In

Croissant is a metadata export format for machine learning datasets that (until this release) was optional and implemented as external exporter. The code has been merged into the main Dataverse code base which means the Croissant format is automatically available in your installation of Dataverse, alongside older formats like Dublin Core and DDI. If you were using the external Croissant exporter, the merged code is equivalent to version 0.1.6. Croissant bugs and feature requests should now be filed against the main Dataverse repo (https://github.com/IQSS/dataverse) and the old repo (https://github.com/gdcc/exporter-croissant) should be considered retired.

As described in the [Discoverability](https://dataverse-guide--12130.org.readthedocs.build/en/12130/admin/discoverability.html#id6) section of the Admin Guide, Croissant is inserted into the "head" of the HTML of dataset landing pages, as requested by the [Google Dataset Search](https://datasetsearch.research.google.com) team so that their tool can filter by datasets that support Croissant. In previous versions of Dataverse, when Croissant was optional and hadn't been enabled, we used the older "Schema.org JSON-LD" format in the "head". If you'd like to keep this behavior, you can use the feature flag [dataverse.legacy.schemaorg-in-html-head](https://dataverse-guide--12130.org.readthedocs.build/en/12130/installation/config.html#dataverse.legacy.schemaorg-in-html-head).

We are aware that the amount of data in the "head" of the HTML can grow quite large for both Croissant and Schema.org JSON-LD. This is especially true of Croissant which exposes variable-level information. We plan to address this in https://github.com/IQSS/dataverse/issues/12123 . We also plan to support Croissant 1.1 in the future and are tracking this at https://github.com/IQSS/dataverse/issues/12014 .

See also #11254 and #12130.

## New Settings

- dataverse.legacy.schemaorg-in-html-head
