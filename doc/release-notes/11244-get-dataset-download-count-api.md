### New API for the SPA, in order to replicate the "classic" download count.
- ``includeMDC`` parameter is optional. False or not included will return the total count of downloads if MDC is not running. Or, if MDCStartDate is set the count returned will be limited to the time prior to the MDCStartDate. Setting ``includeMDC`` to true will ignore any MDCStartDate and return the total download count for the dataset.

Example:
```
   API_TOKEN='xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'
   export DATASET_ID=1
   export includeMDC=False

   curl -s -H "X-Dataverse-key:$API_TOKEN" -X GET http://localhost:8080/api/datasets/$DATASET_ID/download/count
   curl -s -H "X-Dataverse-key:$API_TOKEN" -X GET http://localhost:8080/api/datasets/$DATASET_ID/download/count?includeMDC=true
```
