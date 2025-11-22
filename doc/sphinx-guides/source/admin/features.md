# Features

An overview of Dataverse features can be found at <https://dataverse.org/software-features>. This is a more comprehensive list.

```{contents} Contents:
:local:
:depth: 3
```


## Support for FAIR Data Principles

Findable, Accessible, Interoperable, Reusable.
[More information.](https://scholar.harvard.edu/mercecrosas/presentations/fair-guiding-principles-implementation-dataverse)
## Data citation for datasets and files

EndNote XML, RIS, or BibTeX format at the dataset or file level.
{doc}`More information.</user/find-use-data>`

## OAI-PMH (Harvesting)

Gather and expose metadata from and to other systems using standardized metadata formats: Dublin Core, Data Document Initiative (DDI), OpenAIRE, etc.
{doc}`More information.</admin/harvestclients>`

## APIs for interoperability and custom integrations

Search API, Data Deposit (SWORD) API, Data Access API, Metrics API, Migration API, etc.
{doc}`More information.</api/index>`

## API client libraries

Interact with Dataverse APIs from Python, R, Javascript, Java, and Ruby
{doc}`More information.</api/client-libraries>`

## DataCite integration

DOIs are reserved, and when datasets are published, their metadata is published to DataCite.
{doc}`More information.</admin/discoverability>`

## Login via Shibboleth

Single Sign On (SSO) using your institution's credentials.
{doc}`More information.</installation/shibboleth>`

## Login via ORCID, Google, GitHub, or Microsoft

Log in using popular OAuth2 providers.
{doc}`More information.</installation/oauth2>`

## Login via OpenID Connect (OIDC)

Log in using your institution's identity provider or a third party.
{doc}`More information.</installation/oidc>`

## Internationalization

The Dataverse software has been translated into multiple languages.
{ref}`More information.<i18n>`

## Versioning

History of changes to datasets and files are preserved.
{doc}`More information.</user/dataset-management>`

## Restricted files

Control who can download files and choose whether or not to enable a "Request Access" button.
{ref}`More information.<restricted-files>`

## Embargo

Make content inaccessible until an embargo end date.
{ref}`More information.<embargoes>`

## Custom licenses

CC0 by default but add as many standard licenses as you like or create your own.
{ref}`More information.<license-config>`

## Custom terms of use

Custom terms of use can be used in place of a license or disabled by an administrator.
{ref}`More information.<license-terms>`

## Publishing workflow support

Datasets start as drafts and can be submitted for review before publication.
{ref}`More information.<dataverse-permissions>`

## File hierarchy

Users are able to control dataset file hierarchy and directory structure.
{doc}`More information.</user/dataset-management>`

## File previews

A preview is available for text, tabular, image, audio, video, and geospatial files.
{ref}`More information.<file-previews>`

## Preview and analysis of tabular files

Data Explorer allows for searching, charting and cross tabulation analysis
{ref}`More information.<inventory-of-external-tools>`

## Usage statistics and metrics

Download counters, support for Make Data Count.
{doc}`More information.</admin/make-data-count>`

## Guestbook

Optionally collect data about who is downloading the files from your datasets.
{ref}`More information.<dataset-guestbooks>`

## Fixity checks for files

MD5, SHA-1, SHA-256, SHA-512, UNF.
{ref}`More information.<:FileFixityChecksumAlgorithm>`

## File download in R and TSV format

Proprietary tabular formats are converted into RData and TSV.
{doc}`More information.</user/tabulardataingest/index>`

## Faceted search

Facets are data driven and customizable per collection.
{doc}`More information.</user/find-use-data>`

## Customization of collections

Each personal or organizational collection can be customized and branded.
{ref}`More information.<theme>`

## Private URL

Create a URL for reviewers to view an unpublished (and optionally anonymized) dataset.
{ref}`More information.<previewUrl>`

## Widgets

Embed listings of data in external websites.
{ref}`More information.<dataverse-widgets>`

## Notifications

In app and email notifications for access requests, requests for review, etc.
{ref}`More information.<account-notifications>`

## Schema.org JSON-LD

Used by Google Dataset Search and other services for discoverability.
{ref}`More information.<metadata-export-formats>`

## External tools

Enable additional features not built in to the Dataverse software.
{doc}`More information.</installation/external-tools>`

## External vocabulary

Let users pick from external vocabularies (provided via API/SKOSMOS) when filling in metadata.
{ref}`More information.<using-external-vocabulary-services>`

## Dropbox integration

Upload files stored on Dropbox.
{doc}`More information.</admin/integrations>`

## GitHub integration

A GitHub Action is available to upload files from GitHub to a dataset.
{doc}`More information.</admin/integrations>`

## Integration with Jupyter notebooks

Datasets can be opened in Binder to run code in Jupyter notebooks, RStudio, and other computation environments.
{ref}`More information.<binder>`

## User management

Dashboard for common user-related tasks.
{doc}`More information.</admin/dashboard>`

## Curation status labels

Let curators mark datasets with a status label customized to your needs.
{ref}`More information.<:AllowedCurationLabels>`

## Branding

Your installation can be branded with a custom homepage, header, footer, CSS, etc.
{ref}`More information.<Branding Your Installation>`

## Backend storage on S3 or Swift

Choose between filesystem or object storage, configurable per collection and per dataset.
{doc}`More information.</developers/big-data-support>`

## Direct upload and download for S3

After a permission check, files can pass freely and directly between a client computer and S3.
{doc}`More information.</developers/big-data-support>`

## Export data in BagIt format

For preservation, bags can be sent to the local filesystem, Duraclound, and Google Cloud.
{ref}`More information.<BagIt Export>`

## Post-publication automation (workflows)

Allow publication of a dataset to kick off external processes and integrations.
{doc}`More information.</developers/workflows>`

## Pull header metadata from Astronomy (FITS) files

Dataset metadata prepopulated from FITS file metadata.
{ref}`More information.<fits>`

## Provenance

Upload standard W3C provenance files or enter free text instead.
{ref}`More information.<provenance>`

## Auxiliary files for data files

Each data file can have any number of auxiliary files for documentation or other purposes (experimental).
{doc}`More information.</developers/aux-file-support>`

