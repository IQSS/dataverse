### Optional Language Selection for Collection Metadata Blocks

The Native API endpoint `GET /api/dataverses/{identifier}/metadatablocks` now uses the optional `Accept-Language` header, such as `Accept-Language: fr-CA,fr;q=0.8,en;q=0.6`, to return translated metadata block display names and field titles, display names, descriptions, and watermarks, including compound child fields. The highest-priority language range is used. Translations use the language bundles installed in Dataverse, with fallback to language, default/base bundle or database values when translations are unavailable.

Requests without the header, or with an empty header, retain their current behavior. Identifiers, field definitions, and controlled vocabulary values are unchanged. Malformed headers return HTTP 400. Well-formed ranges without an installed translation return HTTP 200 with fallback text; `und` and `*` use the base bundle. Parsing does not check whether a language is registered.

For more information, see the [Native API Guide](https://guides.dataverse.org/en/latest/api/native-api.html#list-metadata-blocks-defined-on-a-dataverse-collection) and [#12905](https://github.com/IQSS/dataverse/issues/12905).
