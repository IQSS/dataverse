## Notes for Tool Developers and Integrators

### Filenames

Dataverse Installations using S3 storage will no longer replace spaces in file names with the +. If your tool or integration has any special handling around this character change, you can remove it.

(review this note if this is in the same release as the fix for #7188)