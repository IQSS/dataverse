This release enhances how numerical and date fields are indexed in Solr. Previously, all fields were indexed as English text (text_en), but with this update:

* Integer fields are indexed as `plong`
* Float fields are indexed as `pdouble`
* Date fields are indexed as `date_range` (`solr.DateRangeField`)

This enables range queries via the search bar or API, such as `exampleIntegerField:[25 TO 50]` or `exampleDateField:[2000-11-01 TO 2014-12-01]`.

Dataverse administrators must update their Solr schema.xml (manually or by rerunning `update-fields.sh`) and reindex all datasets.

Additionally, search result highlighting is now more accurate, ensuring that only fields relevant to the query are highlighted in search results. If the query is specifically limited to certain fields, the highlighting is now limited to those fields as well.
