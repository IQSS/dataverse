## Notifications API Update

**Endpoint:** `notifications/all`

**Enhancements:**

* When the query parameter `inAppNotificationFormat=true` is set:

    * Notifications of types:

        * `REQUESTFILEACCESS`
        * `REQUESTEDFILEACCESS`
        * `GRANTFILEACCESS`
        * `REJECTFILEACCESS`

  now return both the **dataset display name** and **dataset persistent identifier**.

    * Notifications of type `DATASETMENTIONED` now return a **formatted JSON** in the `additionalInfo` field when this field contains a valid persisted JSON string, instead of a raw JSON string.

Related issue: #11804
Related PR: #11851
