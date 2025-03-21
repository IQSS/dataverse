The "string" type has been added as a new field type for metadata fields.

In contrast to "text" fields, "string" fields are stored and indexed exactly as provided, without any text analysis or transformations.

This field type is suitable for fields like IDs (e.g. ORCIDs) or enums, where exact matches are required when searching.