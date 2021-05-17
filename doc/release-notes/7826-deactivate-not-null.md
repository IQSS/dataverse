### Authenticated User Deactivated Field Updated

The "deactivated" field on the Authenticated User table has been updated to be a non-nullable field. When the field was added in version 5.3 it was set to 'false' in an update script. If for whatever reason that update failed in the 5.3 deploy you will need to re-run it before deploying 5.5. The update query you may need to run is: UPDATE authenticateduser SET deactivated = false WHERE deactivated IS NULL;
