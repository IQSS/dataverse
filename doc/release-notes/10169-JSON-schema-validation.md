### JSON Schema for datasets

Enhanced JSON schema validation with checks for required and allowed child objects, Type checking for field types including: ''primative''; ''compound''; and ''controlledVocabulary'' . More user-friendly error messages to help pinpoint the issues in the Dataset JSON.  Rules are driven off the database schema, so no manual configuration is needed. See [Retrieve a Dataset JSON Schema for a Collection](https://guides.dataverse.org/en/6.1/api/native-api.html#retrieve-a-dataset-json-schema-for-a-collection) in the API Guide and PR #10169.

