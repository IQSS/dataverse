## Ability to Create New Exporters

It is now possible for third parties to develop and share code to provide new metadata export formats for Dataverse. Export formats can be made available via the Dataverse UI and API or configured for use in Harvesting. Dataverse now provides developers with a separate dataverse-spi JAR file that contains the Java interfaces and classes required to create a new metadata Exporter. Once a new Exporter has been created and packaged as a JAR file, administrators can use it by specifying a local directory for third party Exporters, dropping then Exporter JAR there, and restarting Payara. This mechanism also allows new Exporters to replace any of Dataverse's existing metadata export formats.

## Backward Incompatibilities

Care should be taken when replacing Dataverse's internal metadata export formats as third party code, including other third party Exporters may depend on the contents of those export formats. When replacing an existing format, one must also remember to delete the cached metadata export files or run the reExport command for the metadata exports of existing datasets to be updated.

## New JVM/MicroProfile Settings

dataverse.spi.export.directory - specifies a directory, readable by the Dataverse server. Any Exporter JAR files placed in this directory will be read by Dataverse and used to add/replace the specified metadata format.