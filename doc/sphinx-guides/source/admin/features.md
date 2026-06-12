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

````{grid} 3
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

Each data file can have any number of auxiliary files for documentation or other purposes (experimental).
{doc}`More information.</developers/aux-file-support>`

## Geospatial

### Geospatial Metadata Fields

There is a dedicated geospatial metadata block.
{ref}`More information.<metadata-references>`

### Geospatial File Preview

GeoJSON, GeoTIFF, and Shapefiles can be previewed as a map.
{ref}`More information.<geojson>`

### Geospatial Search API

Pass `geo_point` and `geo_radius` to find datasets based on their bounding box.
{doc}`More information.</api/search>`


## Reusability

### Multiple License Support

Users can select from multiple standard and provided custom licenses.
{ref}`More information.<license-config>`

### Custom Terms of Use

Users can write custom terms of use in place of a predefined license.
{ref}`More information.<license-terms>`

### Data Citation Formats

EndNote XML, RIS, BibTeX, or 1000+ CSL formats at the dataset or file level.
{doc}`More information.</user/find-use-data>`

### Provenance

At the file level, upload standard W3C provenance files or enter free text instead.
{ref}`More information.<provenance>`

### Post-Publication Workflows

Allow publication of a dataset to trigger external processes and integrations.
{doc}`More information.</developers/workflows>`
