# License management via API

See https://github.com/IQSS/dataverse/issues/9148. 

When publishing a dataset via API, it now requires the dataset to either have a standard license configured, or have valid Custom Terms of Use (if allowed by the instance). Attempting to publish a dataset without such **will fail with an error message**. This introduces a backward incompatibility, and if you have scripts that automatically create, update and publish datasets, this last step may start failing. Because, unfortunately, there were some problems with the datasets APIs that made it difficult to manage licenses, so an API user was likely to end up with a dataset missing either of the above. In this release we have addressed it by making the following fixes:

We fixed the incompatibility between the format in which license information wad *exported* in json, and the format the create and update APIs were expecting it for *import* (https://github.com/IQSS/dataverse/issues/9155). This means that the following json format can now be imported:
```
"license": {
   "name": "CC0 1.0",
   "uri": "http://creativecommons.org/publicdomain/zero/1.0"
}
```
However, for the sake of backward compatibility the old format
```
"license" : "CC0 1.0"
```
will be accepted as well.

We have added the default license (CC0) to the model json file that we provide and recommend to use as the model in the Native API Guide (https://github.com/IQSS/dataverse/issues/9364).

And we have corrected the misleading language in the same guide where we used to recommend to users that they select, edit and re-import only the `.metadataBlocks` fragment of the json metadata representing the latest version. There are in fact other useful pieces of information that need to be preserved in the update (such as the `"license"` section above). So the recommended way of creating base json for updates via the API is to select *everything but* the `"files"` section, with (for example) the following `jq` command:

```
jq '.data | del(.files)'
```

Please see the [Update Metadata For a Dataset](https://guides.dataverse.org/en/latest/api/native-api.html#update-metadata-for-a-dataset) section of our Native Api guide for more information. 
