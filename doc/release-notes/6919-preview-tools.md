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

Optionally, you can convert existing tools in the `externaltool` database table from type "explore" to "preview". The only known preview tools are the [dataverse-previewers][]. Tools of type "explore" that have `hasPreviewMode` set to true will continue to be displayed in the Preview tab.

[dataverse-previewers]: https://github.com/GlobalDataverseCommunityConsortium/dataverse-previewers

## Notes for Tool Developers and Integrators

### Preview Only External Tools

A new external tool type called "preview" has been added that prevents the tool from being displayed under "Explore Options" under the "Access File" button on the file landing page (#6919).
