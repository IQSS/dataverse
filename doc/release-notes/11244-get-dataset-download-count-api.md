### New API for the SPA, in order to replicate the "classic" download count.
- ``date`` parameter is optional and denotes the cutoff date when Make Data Count logging started.

Example:
```
   API_TOKEN='xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'
   export DATASET_ID=1
   export MDC_DATE=2025-01-01

   curl -s -H "X-Dataverse-key:$API_TOKEN" -X GET http://localhost:8080/api/datasets/$DATASET_ID/download/count
   curl -s -H "X-Dataverse-key:$API_TOKEN" -X GET http://localhost:8080/api/datasets/$DATASET_ID/download/count?date=$MDC_DATE
```
