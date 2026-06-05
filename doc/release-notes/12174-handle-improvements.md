This release offers improved support for Handles as persistent ids.

The following issues are fixed:
- When pid registration of persistent ids for files is enabled, Dataverse will create the handle as soon as the file is created (similary to other persistent id providers) (issue #12174);
- When a new handle is created, for a dataset or file that is still a draft, it will be reserved and registered, but not visible publicly. The handle will become visible and the redirects will start working once it is published. This is also in line with how DOI providers work (issue #8881). 