The initial release of the Dataverse v6.3 introduced a bug where publishing would break the dataset thumbnail, which in turn broke the rendering of the parent Collection ("dataverse") page. This problem was fixed in the PR 10820.

This bug fix will prevent this from happening in the future, but does not fix any existing broken links. To restore any broken thumbnails caused by this bug, you can call the http://localhost:8080/api/admin/clearThumbnailFailureFlag API, which will attempt to clear the flag on all files (regardless of whether caused by this bug or some other problem with the file) or the http://localhost:8080/api/admin/clearThumbnailFailureFlag/id to clear the flag for individual files. Calling the former, batch API is recommended. 

Additionally, the same PR made it possible to turn off the feature that automatically selects of one of the image datafiles to serve as the thumbnail of the parent dataset. An admin can turn it off by raising the feature flag `<jvm-options>-Ddataverse.feature.disable-dataset-thumbnail-autoselect=true</jvm-options>`. When the feature is disabled, a user can still manually pick a thumbnail image, or upload a dedicated thumbnail image.

