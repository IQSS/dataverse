## BUG ##
Search API doesn't return image_url after newly created dataset is published.

The Dataset thumbnail will be created automatically when a Dataset is published under the following conditions: The Dataset has no existing thumbnail; The Dataset has image files that can be converted to a thumbnail; The Feature Flag "disable-dataset-thumbnail-autoselect" is not enabled;
