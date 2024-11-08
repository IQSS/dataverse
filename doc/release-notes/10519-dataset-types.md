## Dataset Types can be linked to Metadata Blocks

Metadata blocks (e.g. "CodeMeta") can now be linked to dataset types (e.g. "software") using new superuser APIs.

This will have the following effects for the APIs used by the new Dataverse UI ( https://github.com/IQSS/dataverse-frontend ):

- The list of fields shown when creating a dataset will include fields marked as "displayoncreate" (in the tsv/database) for metadata blocks (e.g. "CodeMeta") that are linked to the dataset type (e.g. "software") that is passed to the API.
- The metadata blocks shown when editing a dataset will include metadata blocks (e.g. "CodeMeta") that are linked to the dataset type (e.g. "software") that is passed to the API.

The CodeMeta metadata block is now available in the Dockerized development environment.

For more information, see the guides and #10519.
