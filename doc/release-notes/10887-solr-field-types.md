This release enhances how numerical and date fields are indexed in Solr. Previously, all fields were indexed as English text (text_en), but with this update:

* Integer fields are indexed as `plong`
* Float fields are indexed as `pdouble`
* Date fields are indexed as `date_range` (`solr.DateRangeField`)

Specifically, the following fields were updated:

- coverage.Depth
- coverage.ObjectCount
- coverage.ObjectDensity
- coverage.Redshift.MaximumValue
- coverage.Redshift.MinimumValue
- coverage.RedshiftValue
- coverage.SkyFraction
- coverage.Spectral.CentralWavelength
- coverage.Spectral.MaximumWavelength
- coverage.Spectral.MinimumWavelength
- coverage.Temporal.StartTime
- coverage.Temporal.StopTime
- dateOfCollectionEnd
- dateOfCollectionStart
- dateOfDeposit
- distributionDate
- dsDescriptionDate
- journalPubDate
- productionDate
- resolution.Redshift
- targetSampleActualSize
- timePeriodCoveredEnd
- timePeriodCoveredStart

This change enables range queries when searching from both the UI and the API, such as `dateOfDeposit:[2000-01-01 TO 2014-12-31]` or `targetSampleActualSize:[25 TO 50]`.

Dataverse administrators must update their Solr schema.xml (manually or by rerunning `update-fields.sh`) and reindex all datasets.

Additionally, search result highlighting is now more accurate, ensuring that only fields relevant to the query are highlighted in search results. If the query is specifically limited to certain fields, the highlighting is now limited to those fields as well.

## Upgrade Instructions

7\. Update Solr schema.xml file. Start with the standard v6.5 schema.xml, then, if your installation uses any custom or experimental metadata blocks, update it to include the extra fields (step 7a).

Stop Solr (usually `service solr stop`, depending on Solr installation/OS, see the [Installation Guide](https://guides.dataverse.org/en/6.5/installation/prerequisites.html#solr-init-script)).

```shell
service solr stop
```

Replace schema.xml

```shell
wget https://raw.githubusercontent.com/IQSS/dataverse/v6.5/conf/solr/schema.xml
cp schema.xml /usr/local/solr/solr-9.4.1/server/solr/collection1/conf
```

Start Solr (but if you use any custom metadata blocks, perform the next step, 7a first).

```shell
service solr start
```

7a\. For installations with custom or experimental metadata blocks:

Before starting Solr, update the schema to include all the extra metadata fields that your installation uses. We do this by collecting the output of the Dataverse schema API and feeding it to the `update-fields.sh` script that we supply, as in the example below (modify the command lines as needed to reflect the names of the directories, if different):

```shell
	wget https://raw.githubusercontent.com/IQSS/dataverse/v6.5/conf/solr/update-fields.sh
	chmod +x update-fields.sh
	curl "http://localhost:8080/api/admin/index/solr/schema" | ./update-fields.sh /usr/local/solr/solr-9.4.1/server/solr/collection1/conf/schema.xml
```

Now start Solr.

8\. Reindex Solr

Note: these instructions are a little different than usual because we observed a strange error about `DOCS_AND_FREQS_AND_POSITIONS` when testing upgrades (see #11139 for details). Extra steps about explicitly clearing the index and reloading the core are included. If you run into trouble, as a last resort, you could reinstall Solr completely and then reindex.

Clear the Solr index:

```shell
curl http://localhost:8080/api/admin/index/clear
```

Make sure the Solr index is empty:

```shell
curl "http://localhost:8983/solr/collection1/select?rows=1000000&wt=json&indent=true&q=*%3A*"
```

Reload the Solr core:

```shell
curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"
```

Below is the simplest way to reindex Solr:

```shell
curl http://localhost:8080/api/admin/index
```

The API above rebuilds the existing index "in place". If you want to be absolutely sure that your index is up-to-date and consistent, you may consider wiping it clean and reindexing everything from scratch (see [the guides](https://guides.dataverse.org/en/latest/admin/solr-search-index.html)). Just note that, depending on the size of your database, a full reindex may take a while and the users will be seeing incomplete search results during that window.
