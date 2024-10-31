## Feature: Prevent publishing Datasets without files
A new attribute was added to Collections in order to control the publishing of Datasets without files. Once set, the publishing of a Dataset within a Collection or Collection's hierarchy, without files, will be blocked for all non superusers.
In order to configure a Collection to block publishing a superuser must set the attribute "requireFilesToPublishDataset" to true.
Any Collection created under a Collection with this attribute will also be bound by this blocking. Setting this attribute on the Root Dataverse will essentially block the publishing of Datasets without files for the entire installation.
```shell
curl -X PUT -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/dataverses/$ID/attribute/requireFilesToPublishDataset?value=true"
```

See also #10981.
