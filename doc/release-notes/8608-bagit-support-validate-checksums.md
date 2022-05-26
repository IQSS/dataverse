## BagIt Support - Automatic checksum validation on zip file upload
The BagIt file handler detects and transforms zip files with a BagIt package format into Dataverse DataFiles. The system validates the checksums of the files in the package payload as described in the first manifest file with a hash algorithm that we support. Take a look at `BagChecksumType class <https://github.com/IQSS/dataverse/tree/develop/src/main/java/edu/harvard/iq/dataverse/util/bagit/BagChecksumType.java>`_ for the list of the currently supported hash algorithms.

The handler will not allow packages with checksum errors. The first 5 errors will be displayed to the user. This is configurable though database settings.

The checksum validation uses a thread pool to improve performance. This thread pool can be adjusted to your Dataverse installation requirements.

The BagIt file handler is disabled by default. Use the ``:BagItHandlerEnabled`` database settings to enable it: ``curl -X PUT -d 'true' http://localhost:8080/api/admin/settings/:BagItHandlerEnabled``

For more configuration settings see the user guide: https://guides.dataverse.org/en/latest/installation/config.html#bagit-file-handler