## RORs for User Accounts

Users can now associate one or more ROR (Research Organization Registry) identifiers with their account, in order. The first one is the user's primary ROR. Users can add, remove, and reorder their RORs when signing up (including the first login with ORCID, GitHub, Google, or Microsoft) and when editing their account information. See [the guides](https://guides.dataverse.org/en/6.13/user/account.html#account-rors).

When a user creates a dataset, the author "Affiliation" field is now pre-populated with their primary ROR if they have one. Otherwise it is pre-populated with their account's affiliation, as before.

## API Updates

- New endpoints for listing, adding, replacing/reordering, and removing a user's RORs: `GET`, `POST`, and `PUT` on `/api/users/{identifier}/rors`, and `DELETE` on `/api/users/{identifier}/rors/{rorId}`. They can be used by the user themselves (including via `:me`) or by a superuser. See [the guides](https://guides.dataverse.org/en/6.13/api/native-api.html#user-rors).
- The user JSON (for example, from `/api/users/:me`) now includes a `rors` array.
- When accounts are merged, the RORs of the account being merged in are appended after those of the surviving account, skipping duplicates.
