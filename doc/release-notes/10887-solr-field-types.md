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
