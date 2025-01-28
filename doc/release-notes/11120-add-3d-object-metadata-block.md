### New 3D Object Data Metadata Block

A new metadata block has been added for describing 3D object data. You can download it from the [guides](https://dataverse-guide--11167.org.readthedocs.build/en/11167/user/appendix.html). See also #11120 and #11167.

All new Dataverse installations will receive this metadata block by default. We recommend adding it by following the upgrade instructions below.

## Upgrade Instructions

### For 6.6-Release-notes.md

6\. Restart Payara

7\. Update metadata blocks

These changes reflect incremental improvements made to the handling of core metadata fields.

```shell
wget https://raw.githubusercontent.com/IQSS/dataverse/v6.6/scripts/api/data/metadatablocks/citation.tsv

curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file citation.tsv
```
```shell
wget https://raw.githubusercontent.com/IQSS/dataverse/v6.6/scripts/api/data/metadatablocks/3d_objects.tsv

curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file 3d_objects.tsv
```

8\. Update Solr schema.xml file. Start with the standard v6.6 schema.xml, then, if your installation uses any custom or experimental metadata blocks, update it to include the extra fields (step 8a).

Stop Solr (usually `service solr stop`, depending on Solr installation/OS, see the [Installation Guide](https://guides.dataverse.org/en/6.6/installation/prerequisites.html#solr-init-script)).

```shell
service solr stop
```

Replace schema.xml

```shell
wget https://raw.githubusercontent.com/IQSS/dataverse/v6.6/conf/solr/schema.xml
cp schema.xml /usr/local/solr/solr-9.4.1/server/solr/collection1/conf
```

Start Solr (but if you use any custom metadata blocks or adding 3D Objects, perform the next step, 8a first).

```shell
service solr start
```
8a\. For installations with custom or experimental metadata blocks:

Before starting Solr, update the schema to include all the extra metadata fields that your installation uses. We do this by collecting the output of the Dataverse schema API and feeding it to the `update-fields.sh` script that we supply, as in the example below (modify the command lines as needed to reflect the names of the directories, if different):

```shell
	wget https://raw.githubusercontent.com/IQSS/dataverse/v6.6/conf/solr/update-fields.sh
	chmod +x update-fields.sh
	curl "http://localhost:8080/api/admin/index/solr/schema" | ./update-fields.sh /usr/local/solr/solr-9.4.1/server/solr/collection1/conf/schema.xml
```

Now start Solr.

9\. Reindex Solr
