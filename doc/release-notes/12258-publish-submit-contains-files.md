## Bug: Publish and Submit for review must contain files
When `requireFilesToPublishDataset` is set on a Dataverse a Dataset must contain files to be published or submitted for review. This fix make sure the dataset version contains files, and not just the dataset. It also adds this check to the `Submit for Review` functionality.
