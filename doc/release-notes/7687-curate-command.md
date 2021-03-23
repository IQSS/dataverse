## Notes for Dataverse Installation Administrators

### DB Cleanup for Superusers Releasing without Version Updates

In datasets where a superuser has run the Curate command and the update included a change to the fileaccessrequest flag, those changes would not be reflected appropriately in the published version. This should be a rare occurrence.

Instead of an automated solution, we recommend inspecting the affected datasets and correcting the fileaccessrequest flag as appropriate. You can identify the affected datasets this via a query, which is available in the folder here:

https://github.com/IQSS/dataverse/raw/develop/scripts/issues/7687/