The experimental codemeta metadatablock is improved by:
* Adding subfields for size, unit and type to memoryRequirements and subfields for size and unit for storage to storageRequirements of software to improve the machine actionability of these metadata fields and enable external tools like Jupyter labs to run the software in an appropriate environmen
* adding a new subfield InfoUrl to softwareSuggestions and softwareRequirements to distinguish between the download url of a dependency (URL) and an information page of a dependency (InfoUrl)
* adjusting the termURI of the contIntegration metadata field to the changes with CodeMeta v.3.0

Please note, that existing metadata contents of the fields memoryRequirements and storageRequirements have to be manually migrated to the new subfields.