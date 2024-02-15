
`AllowHarvestingMissingCVV` setting to enable/disable allowing datasets to be harvested with Controlled Vocabulary Values that existed in the originating Dataverse Project but are not in the harvesting Dataverse Project.
The default value of this setting is false/no which will cause the harvesting of the dataset to fail.
By activating this feature (true/yes) the value in question will be removed from the list of values and the dataset will be harvested without the missing value.

`curl http://localhost:8080/api/admin/settings/:AllowHarvestingMissingCVV -X PUT -d yes`
