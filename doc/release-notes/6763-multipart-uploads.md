# Large Data Support (continued)

Direct S3 uploads now support multi-part uploading of large files (> 1GB by default) via the user interface and the API (which is used in the [Dataverse Uploader](https://github.com/GlobalDataverseCommunityConsortium/dataverse-uploader)). This allows uploads larger than 5 GB when using Amazon AWS S3 stores.