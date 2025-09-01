# Release Notes

## Fixes
- Added the missing `isDefault` field to the **Get Dataset Templates** API response.
- Resolved an API **500 error** that occurred when working with templates containing custom license terms.
- Updated API behavior so templates are **no longer returned from parent dataverses** when the "Include Templates from Root" option is unchecked in the UI.
- Corrected an issue where the `isDefault` property was always returned as `false` when retrieving a template via the API, even when it was set as default in the UI.  

See #11704
