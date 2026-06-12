# Features

See {doc}`/quickstart/what-is-dataverse` for {ref}`core-capabilities`.

<!--
```{contents} Contents:
#:local:
#:depth: 3
```
-->

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`terminal` Artifical Intelligence
```

```{grid-item-card} AI Tools
A number of AI tools integrate with Dataverse.
+++
{doc}`More information.</ai/index>`
```

```{grid-item-card} Model Context Protocol
Model Context Protocol (MCP) is a standard for AI Agents to communicate with tools and services.
+++
{ref}`More information.<mcp>`
```
````

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`download` Access and Download
```

```{grid-item-card} Faceted Search
Facets are data driven and customizable per collection.
+++
{doc}`More information.</user/find-use-data>`
```

```{grid-item-card} File Previews
A preview is available for text, tabular, image, audio, video, and geospatial files.
+++
{ref}`More information.<file-previews>`
```

```{grid-item-card} Preview URL
Create a URL for reviewers to view an unpublished (and optionally anonymized) dataset.
+++
{ref}`More information.<previewUrl>`
```

```{grid-item-card} Guestbook
Optionally collect data about who is downloading the files from your datasets.
+++
{ref}`More information.<dataset-guestbooks>`
```

```{grid-item-card} Download in Open Tabular Formats
Proprietary tabular formats are converted into TSV and RData for download.
+++
{doc}`More information.</user/tabulardataingest/index>`
```
````

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`manage_accounts` Administration
```

```{grid-item-card} User Management
Dashboard for common user-related tasks.
+++
{doc}`More information.</admin/dashboard>`
```

```{grid-item-card} Quotas
For number of files, amount of storage, etc.
+++
{doc}`More information.</admin/collectionquotas>`
```

```{grid-item-card} Usage Statistics and Metrics
Download counters, support for Make Data Count.
+++
{doc}`More information.</admin/make-data-count>`
```

```{grid-item-card} Configurable Notifications
In-app and email notifications for access requests, requests for review, etc. can be muted.
+++
{ref}`More information.<account-notifications>`
```
````

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`local_police` Authentication
```

```{grid-item-card} Login via Shibboleth
Single Sign On (SSO) using your institution's credentials.
+++
{doc}`More information.</installation/shibboleth>`
```

```{grid-item-card} Login via ORCID, Google, GitHub, or Microsoft
Log in using popular OAuth2 providers.
+++
{doc}`More information.</installation/oauth2>`
```

```{grid-item-card} Login via OpenID Connect (OIDC)
Log in using your institution's identity provider or a third party.
+++
{doc}`More information.</installation/oidc>`
```
````

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`palette` Customization
```

```{grid-item-card} Branding
Your installation can be branded with a custom homepage, header, footer, CSS, etc.
+++
{ref}`More information.<Branding Your Installation>`
```

```{grid-item-card} Internationalization
The Dataverse software has been translated into multiple languages.
+++
{ref}`More information.<i18n>`
```

```{grid-item-card} Customization of Collections
Each personal or organizational collection can be customized and branded.
+++
{ref}`More information.<theme>`
```

```{grid-item-card} Widgets
Embed listings of data in external websites.
+++
{ref}`More information.<dataverse-widgets>`
```
````

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`search`{material-regular}`touch_app`{material-regular}`settings`{material-regular}`recycling` FAIR Data Publication
```

```{grid-item-card} Support for FAIR Data Principles
Findable, Accessible, Interoperable, Reusable.
+++
[More information.](https://web.archive.org/web/20191206043258/https://scholar.harvard.edu/mercecrosas/presentations/fair-guiding-principles-implementation-dataverse)
```

```{grid-item-card} Versioning
History of changes to datasets and files are preserved.
+++
{doc}`More information.</user/dataset-management>`
```

```{grid-item-card} Prepublication Review Support
Datasets start as drafts and can be submitted for review before publication where curators can mark datasets with curation status labels.
+++
{ref}`More information.<dataverse-permissions>`
```

```{grid-item-card} Labels for Traditional Knowledge
Integrate with the Local Contexts platform, enabling the use of Traditional Knowledge and Biocultural Labels, and Notices.
+++
{doc}`More information.</installation/localcontexts>`
```
````

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`rule_folder` File Management
```

```{grid-item-card} File Hierarchy
Users are able to control dataset file hierarchy and directory structure.
+++
{doc}`More information.</user/dataset-management>`
```

```{grid-item-card} Restricted Files
Control who can download files and choose whether or not to enable a "Request Access" button.
+++
{ref}`More information.<restricted-files>`
```

```{grid-item-card} Embargo
Make files inaccessible until an embargo end date.
+++
{ref}`More information.<embargoes>`
```

```{grid-item-card} Retention Periods
Make files inaccessible once the retention period set has passed.
+++
{ref}`More information.<retention-periods>`
```

```{grid-item-card} Metadata Extraction from Files
Populate dataset metadata fields from tabular, NetCDF, HDF5, and FITS files.
+++
{ref}`More information.<file-handling>`
```

```{grid-item-card} Configurable Storage
Choose between filesystem or object storage, configurable per collection and per dataset.
+++
{doc}`More information.</installation/big-data-support>`
```

```{grid-item-card} Direct Upload and Download for S3
After a permission check, files can pass freely and directly between a client computer and S3.
+++
{doc}`More information.</installation/big-data-support>`
```

```{grid-item-card} Fixity Checks for Files
MD5, SHA-1, SHA-256, SHA-512, UNF.
+++
{ref}`More information.<:FileFixityChecksumAlgorithm>`
```

```{grid-item-card} Auxiliary Files for Data Files
Each data file can have any number of auxiliary files for documentation or other purposes (experimental).
+++
{doc}`More information.</developers/aux-file-support>`
```
````

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`map` Geospatial Data Support
```

```{grid-item-card} Geospatial Metadata Fields
There is a dedicated geospatial metadata block.
+++
{ref}`More information.<metadata-references>`
```

```{grid-item-card} Geospatial File Preview
GeoJSON, GeoTIFF, and Shapefiles can be previewed as a map.
+++
{ref}`More information.<geojson>`
```

```{grid-item-card} Geospatial Search API
Pass `geo_point` and `geo_radius` to find datasets based on their bounding box.
+++
{doc}`More information.</api/search>`
```
````

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`api` Integrations
```

```{grid-item-card} DataCite
DOIs are reserved, and when datasets are published, their metadata is published to DataCite.
+++
{doc}`More information.</admin/discoverability>`
```

```{grid-item-card} Handle
Handles are a Persistent ID (PID) that are an alternative to DOIs.
+++
{ref}`More information.<pids-configuration>`
```

```{grid-item-card} Globus
Upload from and download to Dataverse using Globus endpoints.
+++
{ref}`More information.<globus-support>`
```

```{grid-item-card} RSpace
Exchange data and metadata with RSpace (e.g. IGSN ID). For example, a Data Management Plan (DMP) can be uploaded to 
RSpace and updated with the DOI of a Dataverse dataset.
+++
{ref}`More information.<rspace>`
```

```{grid-item-card} GitHub
A GitHub Action is available to upload files from GitHub to a dataset.
+++
{doc}`More information.</admin/integrations>`
```

```{grid-item-card} iRODS
Pull data from an iRODS instance to a Dataverse dataset.
+++
{ref}`More information.<irods>`
```

```{grid-item-card} Dropbox
Upload files stored on Dropbox.
+++
{doc}`More information.</admin/integrations>`
```

```{grid-item-card} Jupyter Notebooks
Datasets can be opened in Binder to run code in Jupyter notebooks, RStudio, and other computation environments.
They can also be previewed in Dataverse itself.
+++
{ref}`More information.<binder>`
```

```{grid-item-card} Galaxy
Import files directly from Dataverse into Galaxy as well as publish datasets containing artifacts
 (Histories, datasets, etc.) from Galaxy to Dataverse.
+++
{ref}`More information.<galaxy-integration>`
```

```{grid-item-card} External Tools
Enable additional features not built in to the Dataverse software.
+++
{doc}`More information.</installation/external-tools>`
```

```{grid-item-card} Additional Integrations
Dataverse integrates with a wide variety of third party systems, some of which are highlighted above. 
+++
For a full list, see {doc}`Integrations</admin/integrations>`.
```
````

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`compare_arrows` Interoperability
```

```{grid-item-card} APIs
Search API, Data Deposit API, Data Access API, Metrics API, Migration API, etc. and client libraries in various languages.
+++
{doc}`More information.</api/index>`
```

```{grid-item-card} OAI-PMH Metadata Harvesting
Serve and harvest metadata to and from other systems (e.g. DataCite, other Dataverse installations, etc.) using standardized metadata formats.
+++
{doc}`More information.</admin/harvestserver>`
```

```{grid-item-card} Schema.org JSON-LD
Used by Google Dataset Search and other services for discoverability.
+++
{ref}`More information.<metadata-export-formats>`
```

```{grid-item-card} Croissant
Export metadata as linked data following the Croissant ontology.
+++
{ref}`More information.<croissant-head>`
```

```{grid-item-card} Signposting
Enable easier machine access to datasets by adding linkset in a Dataverse header.
+++
{ref}`More information.<discovery-sign-posting>`
```

```{grid-item-card} External Vocabulary
Let users pick from external vocabularies (provided via API/SKOSMOS) when filling in metadata.
+++
{ref}`More information.<using-external-vocabulary-services>`
```

```{grid-item-card} BagIt Export
For preservation, bags can be sent to the local filesystem, Duracloud, and Google Cloud.
+++
{ref}`More information.<BagIt Export>`
```

```{grid-item-card} RO-Crate
Export dataset metadata as an ro-crate.json.
+++
{ref}`More information.<metadata-export-formats>`
```
````

````{grid} 1 2 2 3
:gutter: 1

```{grid-item-card}
:text-align: center
:columns: 12
## {material-regular}`open_in_new` Reusability
```

```{grid-item-card} Multiple License Support
Users can select from multiple standard and provided custom licenses.
+++
{ref}`More information.<license-config>`
```

```{grid-item-card} Custom Terms of Use
Users can write custom terms of use in place of a predefined license.
+++
{ref}`More information.<license-terms>`
```

```{grid-item-card} Data Citation Formats
EndNote XML, RIS, BibTeX, or 1000+ CSL formats at the dataset or file level.
+++
{doc}`More information.</user/find-use-data>`
```

```{grid-item-card} Provenance
At the file level, upload standard W3C provenance files or enter free text instead.
+++
{ref}`More information.<provenance>`
```

```{grid-item-card} Post-Publication Workflows
Allow publication of a dataset to trigger external processes and integrations.
+++
{doc}`More information.</developers/workflows>`
```
````
