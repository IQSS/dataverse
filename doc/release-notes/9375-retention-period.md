The Dataverse Software now supports file-level retention periods. The ability to set retention periods, with a minimum duration (in months), can be configured by a Dataverse installation administrator. For more information, see the [Retention Periods section](https://guides.dataverse.org/en/6.3/user/dataset-management.html#retention-periods) of the Dataverse Software Guides.

- Users can configure a specific retention period, defined by an end date and a short reason, on a set of selected files or an individual file, by selecting the 'Retention Period' menu item and entering information in a popup dialog. Retention Periods can only be set, changed, or removed before a file has been published. After publication, only Dataverse installation administrators can make changes, using an API.

- After the retention period expires, files can not be previewed or downloaded (as if restricted, with no option to allow access requests). The file (landing) page and all the metadata remains available.


Release notes should mention that a Solr schema update is needed.
