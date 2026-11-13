### CodeMeta v3.0

The experimental CodeMeta metadata block has been improved by:

* Adding subfields for size, unit and type to memoryRequirements and subfields for size and unit to storageRequirements of software to improve the machine actionability of these metadata fields and enable external tools like Jupyter Lab to run the software in an appropriate environment.
* Adding a new subfield InfoUrl to softwareSuggestions and softwareRequirements to distinguish between the download URL of a dependency (URL) and an information page of a dependency (InfoUrl).
* Adjusting the termURI of the contIntegration metadata field to the changes with CodeMeta v3.0.

Please note that existing metadata contents of the fields memoryRequirements and storageRequirements have to be manually migrated to the new subfields. The following SQL query can help you identify these fields:

`select dvo.identifier, dt.name as name, dfv.value as val from datasetfield as df, datasetfieldtype as dt, datasetfieldvalue as dfv, dvobject as dvo, datasetversion as dv where df.id = dfv.datasetfield_id and df.datasetfieldtype_id = dt.id and dvo.id = dv.dataset_id and df.datasetversion_id = dv.id and name IN ('memoryRequirements', 'storageRequirements');`

You can download the updated CodeMeta block from the [Experimental Metadata](https://dataverse-guide--11087.org.readthedocs.build/en/11087/user/appendix.html#experimental-metadata) section of the User Guide. See also #10859 and #11087.