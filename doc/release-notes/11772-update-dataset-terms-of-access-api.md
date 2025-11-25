## New Endpoint: `/datasets/{id}/access`

A new endpoint has been implemented to manage dataset terms of access for restricted files.

### Functionality
- Updates the terms of access for a dataset by applying it to the draft version.
- If no draft exists, a new one is automatically created.

### Usage

**Custom Terms of Access** – Provide a JSON body with the `customTermsOfAccess` object.
    - All fields are optional **except** if there are restricted files in which case `fileAccessRequest` must be set to true or  `termsOfAccess` must be provided.
