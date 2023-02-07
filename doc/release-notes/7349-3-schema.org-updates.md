The Schema.org metadata export and the schema.org metadata embedded in dataset pages has been updated to improve compliance with Schema.org's schema and Google's recommendations.

Backward compatibility - file entries now have the mimetype reported as 'encodingFormat' rather than 'fileFormat' to better conform with the Schema.org specification for DataDownload entries. Download URLs are now sent for all files unless the dataverse.files.hide-schema-dot-org-download-urls setting is set to true.
