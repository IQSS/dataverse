### External Curation Status Labels

A new :AllowedCurationLabels setting allows a site administrator to define one or more sets of labels that can be applied to a draft Dataset version via the user interface or API to indicate the status of the dataset with respect to an externally defined curation process.

Labels are arbitrary (alphanumeric or spaces, up to 32 characters, e.g. "Author contacted", "Privacy Review", "Awaiting paper publication"). Site admins can select a specific set of labels, or disable this functionality per collection. Anyone who can publish a draft dataset (e.g. curators) can set/change/remove labels (from the set specified for the collection containing the dataset) via the user interface or via an API. The API also would allow external tools to search for, read and set labels on Datasets, providing an integration mechanism. Labels are visible on the Dataset page and in Dataverse collection listings/search results. Internally, the labels have no effect, and at publication, any existing label will be removed. A reporting API call allows admins to get a list of datasets and their curation statuses.


The solr schema must be updated as part of installing the release of Dataverse containing this feature for it to work.
