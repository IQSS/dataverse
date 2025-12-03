### Bug Fix

Editing a controlled vocabulary field (i.e. one with values specified in the field's metadatablock) that only allows a single selection would also update the value in the prior published version if (and only if) the edit was made starting from the published version (versus an existing draft). This is now fixed.
The bug appears to be 11+ years old and previously unreported. As the value in the database was overwritten, there is no simple way to detect if/when this occurred without looking at backups or archival file copies. 
 
