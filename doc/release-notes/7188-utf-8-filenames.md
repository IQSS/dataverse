## Notes for Tool Developers and Integrators

### Spaces in File Names

Dataverse Installations will no longer replace spaces in file names of downloaded files with the + character. If your tool or integration has any special handling around this, you may need to make further adjustments to maintain backwards compatibility while also supporting Dataverse installations on 5.4+.

Note that this follows a change from 5.1 that only corrected this for installations running with S3 storage. This makes the behavior consistent across installations running all types of file storage.