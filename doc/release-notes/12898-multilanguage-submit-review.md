## Support Internationalization for `:SubmitForReviewDatasetDisclaimerText` and `:DatasetSubmitForReviewPopupCustomText`

The settings `:SubmitForReviewDatasetDisclaimerText` and `:DatasetSubmitForReviewPopupCustomText` can now be configured with support to multiple languages available in the Dataverse installation:
```bash

curl -X PUT -d 'The setting text content' $SERVER/api/admin/settings/$SETTING/lang/$LANG
```
