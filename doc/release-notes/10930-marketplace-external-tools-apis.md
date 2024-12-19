## New APIs for External Tools Registration for Marketplace

New API base path /api/externalTools created that mimics the admin APIs /api/admin/externalTools. These new add and delete apis require an authenticated superuser token.

Example:
```
   API_TOKEN='xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'
   export TOOL_ID=1

   curl http://localhost:8080/api/externalTools
   curl http://localhost:8080/api/externalTools/$TOOL_ID
   curl -s -H "X-Dataverse-key:$API_TOKEN" -X POST -H 'Content-type: application/json' http://localhost:8080/api/externalTools --upload-file fabulousFileTool.json
   curl -s -H "X-Dataverse-key:$API_TOKEN" -X DELETE http://localhost:8080/api/externalTools/$TOOL_ID
```
