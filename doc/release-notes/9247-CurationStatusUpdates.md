The External/Curation Status Label mechanism has been enhanced:

- adding tracking of who creates the status label and when,
- keeping a history of past statuses
- updating the CSV report to include the creation time and assigner of a status
- updating the getCurationStatus api call to return a JSON object for the status with label, assigner, and create time
- adding an includeHistory query param for these API calls to allow seeing prior statuses
- adding a facet to allow filtering by curation status (for users able to set them)
- adding the creation time to solr as a pdate to support search by time period, e.g. current status set prior to a give date
- standardizing the language around 'curation status' vs 'external status'
- adding a 'curation-status' class to displayed labels to allow styling
- adding a dataverse.ui.show-curation-status-to-all feature flag that allows users who can see a draft but not publish it to also view the curation status

Due to changes in the solr schema, updating the solr schema and reindexing is required. Background reindexing should be OK.