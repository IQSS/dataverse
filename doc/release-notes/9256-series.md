## Metadata field Series now repeatable
This enhancement allows depositors to define multiple instances of the metadata field Series in the Citation Metadata block.

## Major Use Cases and Infrastructure Enhancements
* Data contained in a dataset may belong to multiple series. Making the field Series repeatable will make it possible to reflect this fact in the dataset metadata. (Issue #9255, PR #9256)

### Additional Upgrade Steps

Update the Citation metadata block:

wget https://github.com/IQSS/dataverse/releases/download/v5.14/citation.tsv
curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @citation.tsv -H "Content-type: text/tab-separated-values"

## Additional Release Steps

1. Replace Solr schema.xml to allow multiple series to be used. See specific instructions below for those installations without custom metadata blocks (1a) and those with custom metadata blocks (1b).

1a.

For installations without Custom Metadata Blocks:

-stop solr instance (usually service solr stop, depending on solr installation/OS, see the Installation Guide

-replace schema.xml

cp /tmp/dvinstall/schema.xml /usr/local/solr/solr-8.11.1/server/solr/collection1/conf

-start solr instance (usually service solr start, depending on solr/OS)

1b.

For installations with Custom Metadata Blocks:

-stop solr instance (usually service solr stop, depending on solr installation/OS, see the Installation Guide

edit the following line to your schema.xml (to indicate that series is now multiValued='true"):

<field name="series" type="string" stored="true" indexed="true" multiValued="true"/>

restart solr instance (usually service solr start, depending on solr/OS)