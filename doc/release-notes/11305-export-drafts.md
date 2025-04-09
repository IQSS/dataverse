### Dataset Metadata Can Be Exported From Draft Datasets (via API)

In previous versions of Dataverse, it was only possible to export metadata from published datasets. It is now possible to export metadata from draft datasets via API as long as you supply an API token that has access to the draft. As before, when exporting metadata from published datasets, only the latest published version is supported. Internal exporters have been updated to work with drafts but external exporters might need to be updated (Croissant definitely does). See "upgrade instructions" below for details. See [the guides](https://dataverse-guide--11398.org.readthedocs.build/en/11398/api/native-api.html#export-metadata-of-a-dataset-in-various-formats), #11305, and #11398.

## Upgrade Instructions

If you are using the Croissant exporter, [update it](https://github.com/gdcc/exporter-croissant) to version 0.1.4 or newer for compatibility with exporting drafts. Other external exporters may need to be updated as well. See https://github.com/gdcc/dataverse-exporters for a list.
