### Improvements to fields that appear in the Citation metadata block

Grammar, style and consistency improvements have been made to the titles, tooltip description text, and watermarks of metadata fields that appear in the Citation metadata block.

This includes fields that dataset depositors can edit in the Citation Metadata accordion (i.e. fields controlled by the citation.tsv and citation.properties files) and fields whose values are system-generated, such as the Dataset Persistent ID, Previous Dataset Persistent ID, and Publication Date fields (controlled by the bundles.properties file).

The changes should provide clearer information to curators, depositors, and people looking for data about what the fields are for.

A new page in the Style Guides called "Text" has also been added. The new page includes a section called "Metadata Text Guidelines" with a link to a Google Doc where the guidelines are being maintained for now since we expect them to be revised frequently.

### Additional Upgrade Steps

Update the Citation metadata block:

- `wget https://github.com/IQSS/dataverse/releases/download/v#.##/citation.tsv`
- `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @citation.tsv -H "Content-type: text/tab-separated-values"`