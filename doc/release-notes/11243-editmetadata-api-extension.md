### Edit Dataset Metadata API extension

- This endpoint now allows removing fields (by sending empty values), as long as they are not required by the dataset.
- New ``sourceLastUpdateTime`` optional query parameter, which prevents inconsistencies by managing updates that
  may occur from other users while a dataset is being edited.

NOTE: This release note was updated to conform to the refactoring of the validation as part of issue #11392
