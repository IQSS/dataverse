## Bug Fix
When calling DELETE "/api/datasets/{id}", of a released dataset, as a superuser, the call will no longer be 'upgraded' to a dataset destroy action. The user will receive an 'unauthorized' response with the message to call the API with /destroy in order to delete the dataset. 
