### Optional Language Selection for Collection Metadata Blocks

The Native API endpoint `GET /api/dataverses/{identifier}/metadatablocks` now accepts an optional `locale` parameter, such as `locale=pt-BR`, to return translated metadata block display names and field titles, display names, descriptions, and watermarks, including compound child fields. Translations use the language bundles installed in Dataverse, with fallback to default bundle or database values when translations are unavailable.

Requests without `locale` retain their current behavior. Identifiers, field definitions, and controlled vocabulary values are unchanged. Empty or malformed locale values, and tags without a language, return HTTP 400.

For more information, see the [Native API Guide](https://guides.dataverse.org/en/latest/api/native-api.html#list-metadata-blocks-defined-on-a-dataverse-collection) and [#12905](https://github.com/IQSS/dataverse/issues/12905).
