### Backward Incompatible Changes

The [Show Role](https://dataverse-guide--11116.org.readthedocs.build/en/11116/api/native-api.html#show-role) API endpoint was returning 401 Unauthorized when a permission check failed. This has been corrected to return 403 Forbidden instead. That is, the API token is known to be good (401 otherwise) but the user lacks permission (403 is now sent). See also the [API Changelog](https://dataverse-guide--11116.org.readthedocs.build/en/11116/api/changelog.html), #10340, and #11116.
