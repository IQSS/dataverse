## Release Highlights

### Feature flag to remove the required "reason" field in the "Return To Author" dialog 

A reason field, that is required to not be empty, was added in v6.2. Installations that handle author communications through email or another system may prefer to not be required to use this new field. v6.2 includes a new 
disable-return-to-author-reason feature flag that can be enabled to drop the reason field from the dialog and make sending a reason optional in the api/datasets/{id}/returnToAuthor call.  
