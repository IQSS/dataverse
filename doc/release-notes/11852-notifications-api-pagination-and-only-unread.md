## Notifications API Update

**Endpoint:** `notifications/all`

The user notifications endpoint has been enhanced with new optional query parameters to allow for more specific and
efficient data retrieval.

**1. Filter by Unread Status**

You can now fetch only unread notifications by using the `onlyUnread` boolean parameter.

* **`onlyUnread`**: (Optional, boolean) When set to `true`, the API will only return notifications that the user has not
  yet marked as read.

**Example:**

```bash
curl -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/notifications/all?onlyUnread=true"
```

**2. Pagination Support**

Pagination is now supported through the limit and offset parameters, allowing you to retrieve notifications in smaller,
manageable chunks.

- **`limit`**: (Optional, integer) Specifies the maximum number of notifications to return.

- **`offset`**: (Optional, integer) Specifies the number of notifications to skip before starting to return results.

Example (Retrieve notifications 11 through 20):

```bash
curl -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/notifications/all?limit=10&offset=10"
```

**3 Breaking Change: API Response Format Updated**

To support pagination and improve consistency across the API, the JSON response format for this endpoint has been
changed. This is a breaking change and will require updates to any client code currently using this endpoint.

**Old Format:**

Previously, the response nested the notification list inside a notifications object within the data field.

```
{
  "status": "OK",
  "data": {
    "notifications": [
      / ... /
    ]
  }
}
```

**New Format:**

The response now includes a top-level totalCount field (required for pagination) and places the notification list
directly in the data field. This flattens the structure and makes it consistent with other paginated endpoints.

```
{
  "status": "OK",
  "totalCount": 2,
  "data": [
    / ... /  
  ]
}
```

Related issue: #11852
Related PR: #11854