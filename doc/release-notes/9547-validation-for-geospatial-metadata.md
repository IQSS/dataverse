Validation has been added for the Geographic Bounding Box values in the Geospatial metadata block. This will prevent improperly defined bounding boxes from being created via the edit page or metadata imports. (issue 9547). This also fixes the issue where existing datasets with invalid geoboxes were quietly failing to get reindexed.

For the "upgrade" steps section:

Update Geospatial Metadata Block

- `wget https://github.com/IQSS/dataverse/releases/download/v6.1/geospatial.tsv`
- `curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file @geospatial.tsv`

