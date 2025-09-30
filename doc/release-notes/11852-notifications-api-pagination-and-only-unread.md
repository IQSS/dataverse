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

Related issue: #11852
Related PR: #11854