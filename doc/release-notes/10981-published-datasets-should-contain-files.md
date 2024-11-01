## Feature: Prevent publishing Datasets without files
A new attribute was added to Collections in order to control the publishing of Datasets without files. 
Once set to "True", the publishing of a Dataset within a Collection, without files, will be blocked for all non superusers.
In order to configure a Collection to block publishing a superuser must set the attribute "requireFilesToPublishDataset" to "True".
The collection's hierarchy will be checked if the collection's "requireFilesToPublishDataset" attribute is not set explicitly to "True" or "False".

```shell
curl -X PUT -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/dataverses/$ID/attribute/requireFilesToPublishDataset?value=true"
```

See also #10981.
