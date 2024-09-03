The initial release of the Dataverse v6.3 introduced a bug where publishing would break the dataset thumbnail, which in turn broke the rendering of the parent Collection ("dataverse") page. This problem was fixed in the PR 10820.

Additionally, the same PR made it possible to turn off the feature that automatically selects of one of the image datafiles to serve as the thumbnail of the parent dataset. An admin can turn it off by raising the feature flag `<jvm-options>-Ddataverse.feature.disable-dataset-thumbnail-autoselect=true</jvm-options>`. When the feature is disabled, a user can still manually pick a thumbnail image, or upload a dedicated thumbnail image.

