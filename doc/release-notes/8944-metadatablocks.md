The API endpoint `/api/metadatablocks/{block_id}` has been extended to include the following fields:

- `controlledVocabularyValues` - All possible values for fields with a controlled vocabulary. For example, the values "Agricultural Sciences", "Arts and Humanities", etc. for the "Subject" field.
- `isControlledVocabulary`:  Whether or not this field has a controlled vocabulary.
- `multiple`: Whether or not the field supports multiple values.
