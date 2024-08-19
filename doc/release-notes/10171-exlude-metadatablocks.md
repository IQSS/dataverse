Extension of API `{id}/versions` and `{id}/versions/{versionId}` with an optional ``excludeMetadataBlocks`` parameter,
that specifies whether the metadataBlocks should be listed in the output. It defaults to ``false``, preserving backward
compatibility. (Note that for a dataset with a large number of versions and/or metadataBlocks having the metadata blocks
included can dramatically increase the volume of the output).
