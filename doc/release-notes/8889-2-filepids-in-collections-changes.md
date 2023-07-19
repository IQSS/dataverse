The default for whether PIDs are registerd for files or not is now false. For those who had file PIDs enabled by default, updating to this release will add the :FilePIDsEnabled = true setting to maintain your existing functionality.

It is now possible to allow File PIDs to be enabled/disabled per collection. See the [:AllowEnablingFilePIDsPerCollection](https://guides.dataverse.org/en/latest/installation/config.html#filepidsenabled) section of the Configuration guide for details.

For example, registration of PIDs for files can be enabled in a specific collection when it is disabled instance-wide. Or it can be disabled in specific collections where it is enabled by default. 