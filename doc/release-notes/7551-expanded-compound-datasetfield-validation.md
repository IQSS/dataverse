## Notes for Dataverse Administrators 

Prior to this release, when defining metadata for compound fields (via their dataset field types), fields could be either be optional or required, i.e. if required you must always have (at least one) value for that field. For example, Author Name being required means you must have at least one Author with an nonempty Author name.

In order to support more robust metadata (and specifically to resolve #7551), we need to allow a third case: Conditionally Required, that is, the field is required if and only if any of its "sibling" fields are entered. For example, Producer Name is now conditionally required in the citation metadata block. A user does not have to enter a Producer, but if they do, they have to enter a Producer Name.

This change required some modifications to how "required" is defined in the metadata .tsv files (for compound fields).

Prior to this release, the value of required for the parent compound field did not matter and so was set to false.

Going forward:

- For optional, the parent compound field would be required = false and all children would be required = false.
- For required, the parent compound field would be required = true and at least one child would be required = true.
- For conditionally required, the parent compound field would be required = false and at least one child would be required = true.

This release updates the citation .tsv file that is distributed with the software for the required parent compound fields (e.g. author), as well as sets Producer Name to be conditionally required. No other distributed .tsv files were updated, as they did not have any required compound values.

**If you have created any custom metadata .tsv files**, you will need to make the same (type of) changes there.

### Additional Upgrade Steps

1. Reload Citation Metadata Block:

   `wget https://github.com/IQSS/dataverse/releases/download/v5.4/citation.tsv`
   `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @citation.tsv -H "Content-type: text/tab-separated-values"`

2. Update any custom metadata blocks (if used):

   For any subfield that has a required value of TRUE, find the corresponding parent field and change its required value to TRUE.

   Note: As there is an accompanying Flyway script that updates the values directly in the database, you do not need to reload these metadata .tsv files via API, unless you make additional changes, e.g set some compound fields to be conditionally required.

### Use Case

Metadata designers can now set subfields of compound fields as **conditionally required**, that is, the field is required if and only if any of its "sibling" fields are entered. For example, Producer Name is now conditionally required in the citation metadata block. A user does not have to enter a Producer, but if they do, they have to enter a Producer Name.
