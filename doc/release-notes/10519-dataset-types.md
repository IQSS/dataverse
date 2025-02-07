## Dataset Types can be linked to Metadata Blocks

Metadata blocks (e.g. "CodeMeta") can now be linked to dataset types (e.g. "software") using new superuser APIs.

This will have the following effects for the APIs used by the new Dataverse UI ( https://github.com/IQSS/dataverse-frontend ):

- The list of fields shown when creating a dataset will include fields marked as "displayoncreate" (in the tsv/database) for metadata blocks (e.g. "CodeMeta") that are linked to the dataset type (e.g. "software") that is passed to the API.
- The metadata blocks shown when editing a dataset will include metadata blocks (e.g. "CodeMeta") that are linked to the dataset type (e.g. "software") that is passed to the API.

Mostly in order to write automated tests for the above, a [displayOnCreate](https://dataverse-guide--11001.org.readthedocs.build/en/11001/api/native-api.html#set-displayoncreate-for-a-dataset-field) API endpoint has been added.

For more information, see the guides ([overview](https://dataverse-guide--11001.org.readthedocs.build/en/11001/user/dataset-management.html#dataset-types), [new APIs](https://dataverse-guide--11001.org.readthedocs.build/en/11001/api/native-api.html#link-dataset-type-with-metadata-blocks)), #10519 and #11001.
