- The optional Croissant exporter has been updated to 0.1.6 to prevent variable names, variable descriptions, and variable types from being exposed for restricted files. See https://github.com/gdcc/exporter-croissant/pull/20 and #11752.

## Upgrade Instructions

### Update Croissant exporter, if enabled, and reexport metadata

If you have enabled the Croissant dataset metadata exporter, you should upgrade to version 0.1.6.

- Stop Payara.
- Delete the old Croissant exporter jar file. It will be located in the directory defined by the `dataverse.spi.exporters.directory` setting.
- Download the updated Croissant jar from https://repo1.maven.org/maven2/io/gdcc/export/croissant/ and place it in the same directory.
- Restart Payara.
- Run reExportAll.
