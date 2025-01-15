### Fixes consequences for not adding some optional fields in update dataverse API

Omitting optional fields inputLevels, facetIds, or metadataBlockNames caused data to be deleted.
This fix no longer deletes data for these fields. Two new flags have been added to the ``metadataBlocks`` Json object to signal the deletion of the data.
- ``inheritMetadataBlocksFromParent: true`` will remove ``metadataBlockNames`` and ``inputLevels`` if the Json objects are omitted.
- ``inheritFacetsFromParent: true`` will remove ``facetIds`` if the Json object is omitted.

For more information, see issue [#11130](https://github.com/IQSS/dataverse/issues/11130)
