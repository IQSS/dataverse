This feature adds a new API to send feedback to the Collection, Dataset, or DataFile's contacts.
Similar to the "admin/feedback" API the "sendfeedback" API sends an email to all the contacts listed for the Dataset. The main differences for this feature are:
1. This API is not limited to Admins
2. This API does not return the email addresses in the "toEmail" and "ccEmail" elements for privacy reasons
3. This API can be rate limited to avoid spamming
4. The body size limit can be configured
5. The body will be stripped of any html code to prevent malicious scripts or links
6. The fromEmail will be validated for correct format

To set the Rate Limiting for guest users (See Rate Limiting Configuration for more details. This example allows 1 send per hour for any guest)
``curl http://localhost:8080/api/admin/settings/:RateLimitingCapacityByTierAndAction -X PUT -d '[{\"tier\": 0, \"limitPerHour\": 1, \"actions\": [\"CheckRateLimitForDatasetFeedbackCommand\"]}]'``

To set the message size limit (example limit of 1080 chars):
``curl -X PUT -d 1080 http://localhost:8080/api/admin/settings/:ContactFeedbackMessageSizeLimit``
