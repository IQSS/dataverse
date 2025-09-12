# Release Notes

## Templates API fixes
- Added the missing `isDefault` field to the **Get Dataset Templates** API response.
- Added the missing `fileAccessRequest` field of terms of use and access to the **Get Dataset Templates** API response.
- Added the missing `contactForAccess` field of terms of use and access to the **Get Dataset Templates** API response.
- Resolved an API **500 error** that occurred when working with templates containing custom license terms.
- Updated API behavior so templates are **no longer returned from parent dataverses** when the "Include Templates from Root" option is unchecked in the UI.

## Notifications API fixes
- Fixed API errors caused by NullPointerException when retrieving notifications without a requestor.

See #11704
