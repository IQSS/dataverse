The updateDataverse API endpoint has been updated to support an "inherit from parent" configuration for metadata blocks, facets, and input levels.

When it comes to omitting any of these fields in the request JSON:

- Omitting ``facetIds`` or ``metadataBlockNames`` causes the Dataverse collection to inherit the corresponding configuration from its parent.
- Omitting ``inputLevels`` removes any existing input levels in the Dataverse collection.

Previously, not setting these fields meant keeping the existing ones in the Dataverse.
