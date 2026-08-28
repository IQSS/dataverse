## Highlights

### Export improvements

- More reliable and memory-efficient DDI export
- Ability to force re-export of only selected formats 


## API updates

### Ability to export only selected formats

An optional query parameter ``formats`` has been added to the ``reExportAll`` and ``reExportDataset``, allowing an administrator to force re-export of only the formats specified.

For example:

`curl "http://localhost:8080/api/admin/metadata/reExportAll?formats=Datacite,croissant"`
`curl "http://localhost:8080/api/admin/metadata/:persistentId/reExportDataset?persistentId=doi:XXXXXX&formats=ddi"`

Please see [Admin Guide](https://guides.dataverse.org/en/latest/admin/metadataexport.html) for more information about the metadata export and re-export APIs. 

## Updates for developers

We have refactored the metadata export code to use the new and improved Data Export framework. Note also that the relevant interface and the accompanying classes have been moved out of the main Dataverse repository and into the dedicated [GDCC project](https://github.com/gdcc/dataverse-spi)). In this release this will result in a measurable improvement in exporting of the DDI format. It should be possible to take advantage of this refactoring to improve the exports of other data-rich formats in future releases.
