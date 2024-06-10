Changed ``api/dataverses/{id}/metadatablocks`` so that setting the query parameter ``onlyDisplayedOnCreate=true`` also returns metadata blocks with dataset field type input levels configured as required on the General Information page of the collection, in addition to the metadata blocks and their fields with the property ``displayOnCreate=true`` (which was the original behavior).

A new endpoint ``api/dataverses/{id}/inputLevels`` has been created for updating the dataset field type input levels of a collection via API.
