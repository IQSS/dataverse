Globus support in Dataverse has been expanded to include support for using file-based Globus endpoints, including the case where files are stored on tape and are not immediately accessible, and for referencing files stored on remote Globus endpoints. Support for using the Globus S3 Connector with an S3 store has been retained but requires changes to the Dataverse configuration. Further details can be found in the [Big Data Support section of the Dataverse Guides](https://guides.dataverse.org/en/latest/developers/big-data-support.html#big-data-support)
- Globus functionality remains 'experimental'/advanced in that it requires significant setup, differs in multiple ways from other file storage mechanisms, and may continue to evolve with the potential for backward incomatibilities.
- The functionality is configured per store and replaces the previous single-S3-Connector-per-Dataverse-instance model
- Adding files to a dataset, and accessing files is supported via the Dataverse user interface through a separate [dataverse-globus app](https://github.com/scholarsportal/dataverse-globus)
- The functionality is also accessible via APIs (combining calls to the Dataverse and Globus APIs)

Backward Incompatibilities:
- The configuration for use of a Globus S3 Connector has changed and is aligned with the standard store configuration mechanism
- The new functionality is incompatible with older versions of the globus-dataverse app and the Globus-related functionality in the UI will only function correctly if a Dataverse 6.1 compatible version of the dataverse-globus app is configured.

New JVM Options:
- A new 'globus' store type and associated store-related options have been added. These are described in the [File Storage Options section of the Dataverse Guides](https://guides.dataverse.org/en/latest/installation/config.html#file-storage-using-a-local-filesystem-and-or-swift-and-or-object-stores-and-or-trusted-remote-stores).

Obsolete Settings: the :GlobusBasicToken, :GlobusEndpoint, and :GlobusStores settings are no longer used
