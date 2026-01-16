### API Response Format Fix for `message` Field

The `message` field in API responses from certain endpoints was incorrectly returned as a nested object (`{"message": {"message": "..."}}`) instead of a plain string (`{"message": "..."}`).

This has been fixed. The following endpoints now return the `message` field as a string, consistent with all other API responses:

- `POST /api/datasets/{id}/add` (when uploading duplicate files)
- `PUT /api/admin/settings`
- `PUT /api/dataverses/{id}`
- `PUT /api/dataverses/{id}/inputLevels`
- `POST /api/admin/savedsearches`
- `PUT /api/harvest/clients/{nickName}`
- `PUT /api/harvest/server/oaisets/{specname}`

**Note:** If you implemented a workaround to handle the nested `message` object, you may need to update your code to expect a plain string instead.
