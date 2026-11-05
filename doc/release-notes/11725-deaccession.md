## Bug Fixes

- When all published versions of a dataset are deaccessioned, its DataCite DOI now changes from "findable" to "registered" and its metadata at DataCite is replaced with placeholder values. The same happens to the DOIs of files that are no longer in any published version, even if other versions of the dataset are still published. The DOIs continue to resolve to the tombstone page but are no longer listed in DataCite Commons and services that harvest from DataCite, such as OpenAIRE. See #11725 and #12828.

## Upgrade Instructions

1. Optionally, update DOIs of datasets deaccessioned before upgrading

   DOIs of datasets that were deaccessioned before upgrading are not changed automatically. If you are using DataCite, updating the PID metadata of a dataset (see [docs](https://guides.dataverse.org/en/6.13/admin/dataverses-datasets.html#update-metadata-for-a-published-dataset-at-the-pid-provider)) or of all datasets (see [docs](https://guides.dataverse.org/en/6.13/admin/dataverses-datasets.html#update-metadata-for-all-published-datasets-at-the-pid-provider)) now also changes the DOIs of deaccessioned datasets and files to "registered". Alternatively, they can be changed to "registered" manually in DataCite Fabrica.
