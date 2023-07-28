The default for whether PIDs are registered for files or not is now false.

Installations where file PIDs were enabled by default will have to add the :FilePIDsEnabled = true setting to maintain the existing functionality.

Add step to install:

  If your installation did not have :FilePIDsEnabled set, you will need to set it to true to keep file PIDs enabled:

  curl -X PUT -d 'true' http://localhost:8080/api/admin/settings/:FilePIDsEnabled



It is now possible to allow File PIDs to be enabled/disabled per collection. See the [:AllowEnablingFilePIDsPerCollection](https://guides.dataverse.org/en/latest/installation/config.html#allowenablingfilepidspercollection) section of the Configuration guide for details.

For example, registration of PIDs for files can now be enabled in a specific collection when it is disabled instance-wide. Or it can be disabled in specific collections where it is enabled by default. 