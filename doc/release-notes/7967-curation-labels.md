### External Curation Status Labels

A new :AllowedCurationLabels setting allows an administrator to define one or more sets of labels that can be applied to a draft Dataset version via the user interface or API to indicate the status of the dataset with respect to an externally defined curation process. 

Labels are arbitrary (alphanumeric or spaces, up to 32 characters, e.g. "Author contacted", "Privacy Review", "Awaiting paper publication"). Anyone who can publish the dataset (e.g. curators) can set/change/remove labels from the defined set via the user interface or via an API. The API also would allow external tools to search for, read and set labels on Datasets, providing an integration mechanism. Labels are visible on the Dataset page and in Dataverse collection listings/search results. Internally, the labels have no effect, and at publication, any existing label will be removed. A reporting API call allows admins to get a list of datasets and their curation statuses.
