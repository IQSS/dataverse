
### Metrics API Bug fixes

Two bugs in the Metrics API have been fixed:

- The /datasets and /datasets/byMonth endpoints could report incorrect values if/when they have been called using the dataLocation parameter (which allows getting metrics for local, remote (harvested), or all datasets) as the metrics cache was not storing different values for these cases. 

- Metrics endpoints who's calculation relied on finding the latest published datasetversion were incorrect if/when the minor version number was > 9. 

When deploying the new release, the [/api/admin/clearMetricsCache](https://guides.dataverse.org/en/latest/api/native-api.html#metrics) API should be called to remove old cached values that may be incorrect.