## Metadata field Production Location now repeatable, facetable, and enabled for Advanced Search
This enhancement allows depositors to define multiple instances of the metadata field Production Location in the Citation Metadata block, users to filter search results using the filter facets, and using the field in the Advanced Search option.

## Major Use Cases and Infrastructure Enhancements
* Data contained in a dataset may have been produced at multiple places. Making the field Production Location repeatable will make it possible to reflect this fact in the dataset metadata. Making the field facetable and enabled for Advanced Search will allow us to customize Dataverse collections more appropriately. (Issue #9253, PR #9254)

### Additional Upgrade Steps

Update the Citation metadata block:

- `wget https://github.com/IQSS/dataverse/releases/download/v5.13/citation.tsv`
- `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @citation.tsv -H "Content-type: text/tab-separated-values"`

## Additional Release Steps

1\. Replace Solr schema.xml to allow multiple production locations to be used. See specific instructions below for those installations without custom metadata blocks (1a) and those with  custom metadata blocks  (1b).

1a\.

For installations without Custom Metadata Blocks:

-stop solr instance (usually service solr stop, depending on solr installation/OS, see the [Installation Guide](https://guides.dataverse.org/en/5.13/installation/prerequisites.html#solr-init-script)

-replace schema.xml

cp /tmp/dvinstall/schema.xml /usr/local/solr/solr-8.11.1/server/solr/collection1/conf

-start solr instance (usually service solr start, depending on solr/OS)


1b\. 

For installations with Custom Metadata Blocks:

-stop solr instance (usually service solr stop, depending on solr installation/OS, see the [Installation Guide](https://guides.dataverse.org/en/5.13/installation/prerequisites.html#solr-init-script)

- edit the following line to your schema.xml (to indicate that productionPlace is now multiValued='true"):

    `<field name="productionPlace" type="string" stored="true" indexed="true" multiValued="true"/>`

- restart solr instance (usually service solr start, depending on solr/OS)

