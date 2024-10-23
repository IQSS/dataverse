## Metadata Blocks Can Be Associated with Dataset Types

Metadata blocks (e.g. "codemeta") can now be associated with dataset types (e.g. "software") using new superuser APIs.

This will have the following effects for the APIs used by the new Dataverse UI:

- The list of fields shown when creating a dataset will include fields marked as "displayoncreate" (in the tsv/database) for metadata blocks (e.g. "codemeta") that are associated with the dataset type (e.g. "software") that is passed to the API.
- The metadata blocks shown when editing a dataset will include metadata blocks (e.g. "codemeta") that are associated with the dataset type (e.g. "software") that is passed to the API.

For more information, see the guides and #10519.
