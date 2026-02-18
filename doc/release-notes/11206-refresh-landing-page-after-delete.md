## Bug Fix ##

JSF UI will no longer display the deleted Dataset/Dataverse. A 1 second delay in the UI page redirect gives Solr time to re-index and remove the deleted object.

See:
- [#11206](https://github.com/IQSS/dataverse/issues/11206)  
