### Curation Status Labels

A new :AllowedCurationLabels setting allows a sysadmins to define one or more sets of labels that can be applied to a draft Dataset version via the user interface or API to indicate the status of the dataset with respect to a defined curation process.

Labels are completely customizable (alphanumeric or spaces, up to 32 characters, e.g. "Author contacted", "Privacy Review", "Awaiting paper publication"). Superusers can select a specific set of labels, or disable this functionality per collection. Anyone who can publish a draft dataset (e.g. curators) can set/change/remove labels (from the set specified for the collection containing the dataset) via the user interface or via an API. The API also would allow external tools to search for, read and set labels on Datasets, providing an integration mechanism. Labels are visible on the Dataset page and in Dataverse collection listings/search results. Internally, the labels have no effect, and at publication, any existing label will be removed. A reporting API call allows admins to get a list of datasets and their curation statuses.

The Solr schema must be updated as part of installing the release of Dataverse containing this feature for it to work.

## Additional Release Steps

1\. Replace Solr schema.xml to allow Curation Labels to be used. See specific instructions below for those installations with custom metadata blocks (1a) and those without (1b).

1a\.

For installations with Custom Metadata Blocks:

-stop solr instance (usually service solr stop, depending on solr installation/OS, see the [Installation Guide](https://guides.dataverse.org/en/5.7/installation/prerequisites.html#solr-init-script)

- add the following line to your schema.xml:

    <field name="externalStatus" type="string" stored="true" indexed="true" multiValued="false"/>

- restart solr instance (usually service solr start, depending on solr/OS)

1b\. 

For installations without Custom Metadata Blocks:

-stop solr instance (usually service solr stop, depending on solr installation/OS, see the [Installation Guide](https://guides.dataverse.org/en/5.7/installation/prerequisites.html#solr-init-script)

-replace schema.xml

cp /tmp/dvinstall/schema.xml /usr/local/solr/solr-8.8.1/server/solr/collection1/conf

-start solr instance (usually service solr start, depending on solr/OS)