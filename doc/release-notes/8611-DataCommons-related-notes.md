# Dataverse Software 5.12

This release brings new features, enhancements, and bug fixes to the Dataverse Software. Thank you to all of the community members who contributed code, suggestions, bug reports, and other assistance across the project.

## Release Highlights

### Harvard Data Commons Additions

As reported at the 2022 Dataverse Community Meeting, the Harvard Data Commons project has supported a wide range of additions to the Dataverse software that improve support for Big Data, Workflows, Archiving, and Interaction with other repositories. In many cases, these additions build upon features developed within the Dataverse community by Borealis, DANS, QDR, and TDL and others. Highlights from this work include:

- Initial support for Globus file transfer to upload to and download from a Dataverse managed S3 store. The current implementation disables file restriction and embargo on Globus-enabled stores.
 - ```
- Initial support for Remote File Storage. This capability, enabled via a new RemoteOverlay store type, allows a file stored in a remote system to be added to a dataset (currently only via API) with download requests redirected to the remote system. Use cases include referencing public files hosted on external web servers as well as support for controlled access managed by Dataverse (e.g. via restricted and embargoed status) and/or by the remote store.
- Workflow (add Aday's notes here or reword to separate the Objective 2 work)
- Support for Archiving to any S3 store using Dataverse's RDA-conformant BagIT file format (a BagPack).
- Improved error handling and performance in archival bag creation and new options such as only supporting archiving of one dataset version.
- Additions/corrections to the OAI-ORE metadata format (which is included in archival bags) such as referencing the name/mimeType/size/checksum/download URL or the original file for ingested files, the inclusion of metadata about the parent collection(s) of an archived dataset version and use of the URL form of PIDs.
- Display of archival status within the dataset page versions table, richer status options including success, pending, and failure states, with a complete API for managing archival status.
- Support for batch archiving via API as an alternative to the current options of configuring archiving upon publication or archiving each dataset version manually.
- Initial support for sending and receiving Linked Data Notification messages indicating relationships between a dataset and external resources (e.g. papers or other dataset) that can be used to trigger additional actions, such as the creation of a back-link to provide, for example, bi-directional linking between a published paper and a Dataverse dataset.



## Major Use Cases and Infrastructure Enhancements

Changes and fixes in this release include:

- Administrators can configure an S3 store used in Dataverse to support users uploading/downloading files via Globus File Transfer (PR #8891)
- Administrators can configure a RemoteOverlay store to allow files that remain hosted by a remote system to be added to a dataset. (PR #7325)
- Administrators can configure the Dataverse software to send archival Bag copies of published dataset versions to any S3-compatible service. (PR #8751)
- Users can see information about a dataset's parent collection(s) in the OAI-ORE metadata export. (PR #8770)
- Users and Administrators can now use the OAI-ORE metadata export to retrieve and assess the fixity of the the original file (for ingested tabular files) via the included checksum. (PR #8901)
- Archiving via RDA-conformant Bags is more robust and is more configurable (PR #8773, #8747, #8699, #8609, #8606, #8610)
- Users and administrators can see the archival status of the versions of the datasets they manage in the dataset page version table (PR #8748, #8696)
- Administrators can configure messaging between their Dataverse installation and other repositories that may hold related resources or services interested in activity within that installation (PR #8775)

## Notes for Dataverse Installation Administrators

### Enabling experimental capabilities

Several of the capabilities introduced in v5.12 are "experimental" in the sense that further changes and enhancements to these capabilities should be expected and that these changes may involve additional work, for those who use the initial implementations, when upgrading to newer versions of the Dataverse software. Administrators wishing to use them are encouraged to stay in touch, e.g. via the Dataverse Community Slack space, to understand the limits of current capabilties and to plan for future upgrades.

## New JVM Options and DB Settings

The following DB settings have been added:

- `:LDNMessageHosts`
- `:BasicGlobusToken`
- `:GlobusEndpoint`
- `:GlobusStores`
- `:GlobusAppUrl`
- `:S3ArchiverConfig`
- `:S3ArchiverProfile`
- `:DRSArchivalConfig`

See the [Database Settings](https://guides.dataverse.org/en/5.12/installation/config.html#database-settings) section of the Guides for more information.

## Notes for Developers and Integrators

See the "Backward Incompatibilities" section below.

## Backward Incompatibilities

### OAI-ORE and Archiving Changes

The Admin API call to manually sumbit a dataset version for archiving has changed to require POST instead of GET and to have a name making it clearer that archiving is being done for a given dataset version: /api/admin/submitDatasetVersionToArchive.

Earlier versions of the archival bags included the ingested (tab-separated-value) version of tabular files while providing the checksum of the original file (Issue #8449). This release fixes that by including the original file and its metadata in the archival bag. This means that archival bags created prior to this version do not include a way to validate ingested files. Further, it is likely that capabilities in development (i.e. as part of the [Dataverse Uploader](https://github/org/GlobalDataverseCommunityConsortium/dataverse-uploader) to allow re-creation of a dataset version from an archival bag will only be fully compatible with archival bags generated by a Dataverse instance at a release > v5.12. (Specifically, at a minimum, since only the ingested file is included in earlier archival bags, an upload via DVUploader would not result in the same original file/ingested version as in the original dataset.) Administrators should be aware that re-creating archival bags, i.e. via the new batch archiving API, may be advisable now and will be recommended at some point in the future (i.e. there will be a point where we will start versioning archival bags and will start maintaining backward compatibility for older versions as part of transitioning this from being an experimental capability).

## Complete List of Changes



## Installation

If this is a new installation, please see our [Installation Guide](https://guides.dataverse.org/en/5.12/installation/). Please also contact us to get added to the [Dataverse Project Map](https://guides.dataverse.org/en/5.12/installation/config.html#putting-your-dataverse-installation-on-the-map-at-dataverse-org) if you have not done so already.

## Upgrade Instructions

8\. Re-export metadata files (OAI_ORE is affected by the PRs in these release notes). Optionally, for those using the Dataverse software's BagIt-based archiving, re-archive dataset versions archived using prior versions of the Dataverse software. This will be recommended/required in a future release.

