### Enhancements to DDI Metadata Exports

Several changes have been made to the DDI exports to improve support for internationalization and to improve compliance with CESSDA requirements. These changes include:
* Addition of a holdings element with a URI attribute whose value is the URL form of the dataset PID
* Change to use the URL form of the dataset PID in the \<studydsc\> IDNo element
* Addition of xml:lang attributes specifying the dataset metadata language at the document level and for individual elements such as title and description
* Specification of controlled vocabulary terms in duplicate elements in multiple languages (in the installation default langauge and, if different, the dataset metadata language)

While these changes are intended to improve harvesting and integration with external systems, the could break existing connections that make assumptions about the elements and attributes that have been changed.
