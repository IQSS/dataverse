# getAllNotificationsForUser API extension

- Extended endpoint getAllNotificationsForUser(``/notifications/all``), which now supports an optional query parameter ``inAppNotificationFormat`` which, if sent as ``true``, retrieves the fields needed to build the in-app notifications for the Notifications section of the Dataverse UI, omitting fields related to email notifications. See also #11648 and #11696.

# Notifications triggered by API endpoints

The addDataset and addDataverse API endpoints now trigger user notifications upon successful execution. See also #1342 and #11696.
