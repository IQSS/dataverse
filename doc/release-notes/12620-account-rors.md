## RORs for User Accounts

Users can now associate one or more ROR (Research Organization Registry) identifiers with their account, in order. The first one is the user's primary ROR. For now, RORs are managed through the API only.

## API Updates

- New endpoints for listing, adding, replacing/reordering, and removing a user's RORs: `GET`, `POST`, and `PUT` on `/api/users/{identifier}/rors`, and `DELETE` on `/api/users/{identifier}/rors/{rorId}`. They can be used by the user themselves (including via `:me`) or by a superuser. See [the guides](https://guides.dataverse.org/en/6.13/api/native-api.html#user-rors).
- The user JSON (for example, from `/api/users/:me`) now includes a `rors` array.
- When accounts are merged, the RORs of the account being merged in are appended after those of the surviving account, skipping duplicates.
