### Support for Data Embargoes

Dataverse now supports file-level embargoes. The ability to set embargoes, up to a maximum duration (in months) can be configured by an administrator.

* Users can configure a specific embargo, defined by an end date and a short reason, on a set of selected files or an individual file, by selecting the 'Embargo' menu item and entering information in a popup dialog.
Embargoes can only be set/changed/removed before a file has been published. (Administrators can use a privileged API to change embargoes after publication.)

* While embargoed, files cannot be previewed or downloaded (as if restricted, with no option to allow access requests). After the embargo expires, files become accessible. (If the were also restricted, they remain inacessible and functionality is the same as for any restricted file.)

* By default, the citation date reported for the dataset and the datafiles in version 1.0 reflect the longest embargo period on any file in version 1.0, which is consistent with recommended practice from DataCite. Administrators can still specify an alternate date field to be used in the citation date via the <blank> setting.
