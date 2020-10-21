## Release Highlights

### File Preview When Guestbooks or Terms Exist

Previously, file preview was only available when files were publicly downloadable. Now if a guestbook or terms (or both) are configured for the dataset, they will be shown in the Preview tab and once they are agreed to, the file preview will appear (#6919).

### Preview Only External Tools

A new external tool type called "preview" has been added that prevents the tool from being displayed under "Explore Options" under the "Access File" button on the file landing page (#6919).


## Major Use Cases

Newly-supported use cases in this release include:

- Users can now preview files that have a guestbook or terms. (Issue #6919)
- External tools authors can indicate that their tool is "preview only". (Issue #6919)


## Notes for Dataverse Installation Administrators

### Converting Explore External Tools to Preview Only

When the war file is deployed, a SQL migration script will convert [dataverse-previewers][] to have both "explore" and "preview" types so that they will continue to be displayed in the Preview tab.

If you would prefer that these tools be preview only, you can delete the tools, adjust the JSON manifests (changing "explore" to "preview"), and re-add them.

[dataverse-previewers]: https://github.com/GlobalDataverseCommunityConsortium/dataverse-previewers

## Notes for Tool Developers and Integrators

### Preview Only External Tools

A new external tool type called "preview" has been added that prevents the tool from being displayed under "Explore Options" under the "Access File" button on the file landing page (#6919). This "preview" type replaces "hasPreviewMode", which has been removed.

### Multiple Types for External Tools

External tools now support multiple types. In practice, the types "explore" and "preview" are the only combination that makes a difference in the UI as opposed to only having only one or the other type (see "preview only" above). Multiple types are specified in the JSON manifest with an array in "types". The older, single "type" is still supported but should be considered deprecated.
