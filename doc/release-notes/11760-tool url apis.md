New API calls have been added to retrieve the URLs needed to launch external tools on specific datasets and files:

/api/datasets/$DATASET_ID/externalTool/$TOOL_ID/toolUrl
and
/api/files/$FILE_ID/externalTool/$TOOL_ID/toolUrl

If the dataset/file is not public, the caller must authenticate and have permission to view the dataset/file. In such cases, the generated URL will include a callback token containing a signed URL the tool can use to retrieve all the parameters it is configured for.