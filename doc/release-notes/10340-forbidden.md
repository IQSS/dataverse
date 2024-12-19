### API Now Returns 403 Forbidden for Permission Checks

Dataverse was returning 401 Unauthorized when a permission check failed. This has been corrected to return 403 Forbidden in these cases. That is, the API token is known to be good (401 otherwise) but the user lacks permission (403 is now sent). See also #10340 and #11116.

### Backward Incompatible Changes

See "API Now Returns 403 Forbidden for Permission Checks" above.
