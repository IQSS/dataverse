Prior to this release, when defining metadata for compound fields, fields could be either be optional or required, i.e. if required you must always have (at least one) value for that field. For example, Author Name being required means you must have at least one Author with an nonempty Author name.

In order to support more robust metadata (and specifically to resolve #7551), we need to allow a third case: Conditionally Required, that is, the field is required if and only if any of its "sibling" fields are required. For example, a user does not have to enter a Producer, but if they do, they have to enter a Producer Name.

This change required some modifications to how "required" is defined in the metadata .tsv files (for compound fields).

Prior to this release, the value of required for the parent compound field did not matter and so was set to false.

Going forward:<br>
- For optional, the parent compound field would be required = false and all children would be required = false.<br>
- For required, the parent compound field would be required = true and at least one child would be required = true.<br>
- For conditionally required, the parent compound field would be required = false and at least one child would be required = true.

This release updates the citation .tsv file that us distributed with the software (no other distributed .tsv file has any required values). **If you have created any custom metadata .tsv files**, you will need to make the same (type of) changes there.

For any subfield that has a required value of TRUE, find the corresponding parent field and change its required value to TRUE. 

Note: The metadata .tsv files do not have to be reloaded via API, as there is an accompanying flyway script that updates the values directly in the database.

