## New Endpoint: `/datasets/{id}/license`

A new endpoint has been implemented to manage dataset licenses.

### Functionality
- Updates the license of a dataset by applying it to the draft version.
- If no draft exists, a new one is automatically created.

### Usage
This endpoint supports two ways of defining a license:
1. **Predefined License** – Provide the license name (e.g., `CC BY 4.0`).
2. **Custom Terms of Use and Access** – Provide a JSON body with the `customTerms` object.
    - All fields are optional **except** `termsOfUse`, which is required.
