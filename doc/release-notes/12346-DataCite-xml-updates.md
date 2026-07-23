This release updates the DataCite XML metadata format to 
- indicate compliance with the version 4.7 schema, 
- add support for specifying a 'Translator' contributor,
- add a valueURI attribute to a subject element when a value exists in the keywordTermURI field,
- add a language element when a dataset has one language defined in its Citation block metadata,
- accept dates of the form YYYY or YYYY-MM in the timePeriodCovered and dateOfCollection fields, and
- avoids sending the word 'null' as part of a date range when the start or end date is unspecified.

As it adds Translator to the contributorTypes allowed in the citation block, people would have to reload the block to get the new option.