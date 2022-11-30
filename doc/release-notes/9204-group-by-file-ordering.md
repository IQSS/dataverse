### Support for Grouping Dataset Files by Folder and Category Tag

Dataverse now supports grouping dataset files by folder and/or optionally by Tag/Category. The latter must be enabled by an administrator via the :CategorySortOrder parameter which is used to specify which tags appear first (e.g. to put Documentation files before Data or Code files, etc.) These Group-By options work with the existing sort options, i.e. sorting alphabetically means that files within each folder or tag group will be sorted alphabetically.

### New Setting

:CategorySortOrder - a comma separated list of Category/Tag names defining the order in which files with those tags should be displayed. The setting can include custom tag names along with the pre-defined Documentation, Data, and Code tags.