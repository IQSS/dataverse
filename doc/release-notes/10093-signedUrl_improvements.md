A new version of the standard Dataverse Previewers from https://github/com/gdcc/dataverse-previewers is available. The new version supports the use of signedUrls rather than API keys when previewing restricted files (including files in draft dataset versions). Upgrading is highly recommended.

SignedUrls can now be used with PrivateUrl access tokens, which allows PrivateUrl users to view previewers that are configured to use SignedUrls. See #10093.

Launching a dataset-level configuration tool will automatically generate an API token when needed. This is consistent with how other types of tools work. See #10045.
