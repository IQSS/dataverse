This release adds support for generating citations in any of the standard independent formats specified using the [Citation Style Language](https://citationstyles.org/).
The CSL formats are available to copy/paste from a new "Cite Dataset" menu "View Styles Citations" pop-up the dataset page.
An API call to retrieve a dataset citation in EndNote, RIS, BibTeX, and CSLJson format has also been added. (The first three have been available as downloads from the UI but have not been directly accessible via API. The CSLJson format is new to Dataverse and can be used with open-source libraries to generate all of the other CSL stypes citations.)

Admins can use a new dataverse.csl.common-styles setting to highlight commonly used styles - common styles are listed in the pop-up, others can be found by type-ahead search in a list of 1000+ options.
