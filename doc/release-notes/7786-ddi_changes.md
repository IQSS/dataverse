Note: These notes cover several related PRs (#7958, #7959 respectively for the three bullets below.) If some are not merged before the next release, these notes should be adjusted.

### Enhancements to DDI Metadata Exports

Several changes have been made to the DDI exports to improve support for internationalization and to improve compliance with CESSDA requirements. These changes include:

* Addition of xml:lang attributes specifying the dataset metadata language at the document level and for individual elements such as title and description
* Specification of controlled vocabulary terms in duplicate elements in multiple languages (in the installation default langauge and, if different, the dataset metadata language)

While these changes are intended to improve harvesting and integration with external systems, the could break existing connections that make assumptions about the elements and attributes that have been changed.
