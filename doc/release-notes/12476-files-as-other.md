When files are given Persistent Identifiers (PIDs) such as a DOI, the DataCite export resourceTypeGeneral is now "Other" instead of "Dataset". Dataverse uses "Dataset" to mean a container for files. For files, "Other" is not ideal, but it's a step as we work with DataCite in https://github.com/datacite/datacite-suggestions/discussions/214 to define an appropriate value for dataset files. See [the guides](https://guides.dataverse.org/en/6.13/installation/config.html#filepidsenabled), #5086, #12476, and #12713.

## Upgrade Instructions

1. Optionally, update metadata for files with DataCite DOIs

   If you are using DataCite as a Persistent ID provider and have files that have DOIs, you may want to update their metadata records to pick up the change from "Dataset" to "Other" for resourceTypeGeneral (see #12713).

   We recommend experimenting with a single dataset first (see [docs](https://guides.dataverse.org/en/6.13/admin/dataverses-datasets.html#update-metadata-for-a-published-dataset-at-the-pid-provider)). Depending on how many DOIs need to be changed, you might want to iterate though datasets one by one but an API endpoint is available to process all datasets at once (see [docs](https://guides.dataverse.org/en/6.13/admin/dataverses-datasets.html#update-metadata-for-all-published-datasets-at-the-pid-provider)).

   