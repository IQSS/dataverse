Rate Limiting
=============

.. contents:: Contents:
	:local:

Configuration
-------------

Rate limiting is used to prevent users from over taxing the system either deliberately or by runaway automated processes.
Rate limiting can be configured on a tier level with tier 0 being reserved for guest users and tiers 1-any for authenticated users. New users are defaulted to tier 1.
Superuser accounts are exempt from rate limiting.
Rate limits can be imposed on command APIs by configuring the tier, the command, and the hourly limit in the database.
Two database settings configure the rate limiting.
Note: If either of these settings exist in the database rate limiting will be enabled (note that a Payara restart is required for the setting to take effect). If neither setting exists rate limiting is disabled.

- :RateLimitingDefaultCapacityTiers is the number of calls allowed per hour if the specific command is not configured. The values represent the number of calls per hour per user for tiers 0,1,...
  A value of -1 can be used to signify no rate limit. Tiers not specified in this setting will default to `-1` (No Limit). I.e., -d "10000" is equivalent to -d "10000,-1,-1,..."

.. code-block:: bash

  curl http://localhost:8080/api/admin/settings/:RateLimitingDefaultCapacityTiers -X PUT -d '10000,20000'

- :RateLimitingCapacityByTierAndAction is a JSON object specifying the rate by tier and a list of actions (commands). This allows for more control over the rate limit of individual API command calls.
  In the following example, calls made by a guest user (tier 0) for API GetLatestPublishedDatasetVersionCommand is further limited to only 10 calls per hour, while an authenticated user (tier 1) will be able to make 30 calls per hour to the same API.

:download:`rate-limit-actions.json </_static/installation/files/examples/rate-limit-actions-setting.json>`  Example JSON for RateLimitingCapacityByTierAndAction

.. code-block:: bash

  curl http://localhost:8080/api/admin/settings/:RateLimitingCapacityByTierAndAction -X PUT -d '[{"tier": 0, "limitPerHour": 10, "actions": ["GetLatestPublishedDatasetVersionCommand", "GetPrivateUrlCommand", "GetDatasetCommand", "GetLatestAccessibleDatasetVersionCommand"]}, {"tier": 0, "limitPerHour": 1, "actions": ["CreateGuestbookResponseCommand", "UpdateDatasetVersionCommand", "DestroyDatasetCommand", "DeleteDataFileCommand", "FinalizeDatasetPublicationCommand", "PublishDatasetCommand"]}, {"tier": 1, "limitPerHour": 30, "actions": ["CreateGuestbookResponseCommand", "GetLatestPublishedDatasetVersionCommand", "GetPrivateUrlCommand", "GetDatasetCommand", "GetLatestAccessibleDatasetVersionCommand", "UpdateDatasetVersionCommand", "DestroyDatasetCommand", "DeleteDataFileCommand", "FinalizeDatasetPublicationCommand", "PublishDatasetCommand"]}]'

Statistics
----------

In order to monitor the rate limiting cache for investigative purposes there is a stats endpoint which returns CSV formatted text. The CSV contains multiple lists.
The first list contains the username:command, and number of tokens remaining. This list is sorted by the values and has the header "#<username>:<command>, <available tokens>".
The second list contains username:command, last updated timestamp in minutes, and delta minutes before now. The header for this list is "#<username>:<command>, <timestamp>, <delta minutes> ## deltaMinutesFilter=1".
This list can be filtered to show only the entries with updates within the deltaMinutesFilter requested. ("## deltaMinutesFilter=n" will be added to the header when the filter is included in the call.

.. code-block:: bash

  curl http://localhost:8080/api/admin/rateLimitStats
  curl http://localhost:8080/api/admin/rateLimitStats?deltaMinutesFilter=10
