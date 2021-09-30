### Curation Status Labels

A new :AllowedCurationLabels setting allows a sysadmins to define one or more sets of labels that can be applied to a draft Dataset version via the user interface or API to indicate the status of the dataset with respect to a defined curation process.

Labels are completely customizable (alphanumeric or spaces, up to 32 characters, e.g. "Author contacted", "Privacy Review", "Awaiting paper publication"). Superusers can select a specific set of labels, or disable this functionality per collection. Anyone who can publish a draft dataset (e.g. curators) can set/change/remove labels (from the set specified for the collection containing the dataset) via the user interface or via an API. The API also would allow external tools to search for, read and set labels on Datasets, providing an integration mechanism. Labels are visible on the Dataset page and in Dataverse collection listings/search results. Internally, the labels have no effect, and at publication, any existing label will be removed. A reporting API call allows admins to get a list of datasets and their curation statuses.

The solr schema must be updated as part of installing the release of Dataverse containing this feature for it to work.

## Additional Release Steps

1\. Run the script updateSchemaMDB.sh to generate updated solr schema files and preserve any other custom fields in your Solr configuration.

For example: (modify the path names as needed)

cd /usr/local/solr-8.8.1/server/solr/collection1/conf
wget https://github.com/IQSS/dataverse/releases/download/v5.7/updateSchemaMDB.sh
chmod +x updateSchemaMDB.sh
./updateSchemaMDB.sh -t .

See <https://guides.dataverse.org/en/5.7/admin/metadatacustomization.html#updating-the-solr-schema> for more information.