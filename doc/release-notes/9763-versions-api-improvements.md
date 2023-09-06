# Some improvements have been added to the /versions API

- optional pagination has been added to `/api/datasets/{id}/versions` that may be useful in datasets with a large number of versions;
- a new flag `includeFiles` is added to both `/api/datasets/{id}/versions` and `/api/datasets/{id}/versions/{vid}` (true by default), providing an option to drop the file information from the output;
- when files are requested to be included, some database lookup optimizations have been added to improve the performance on datasets with large numbers of files. 

This is reflected in the [Dataset Versions API](https://guides.dataverse.org/en/9763-lookup-optimizations/api/native-api.html#dataset-versions-api) section of the Guide.

