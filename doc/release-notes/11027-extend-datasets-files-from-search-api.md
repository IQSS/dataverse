### Feature to extend Search API for SPA

Added new fields to search results type=files

For Files:
- restricted: boolean
- canDownloadFile: boolean ( from file user permission)
- categories: array of string "categories" would be similar to what it is in metadata api.
For tabular files:
- tabularTags: array of string for example,{"tabularTags" : ["Event", "Genomics", "Geospatial"]}
- variables: number/int shows how many variables we have for the tabular file
- observations: number/int shows how many observations for the tabular file



New fields added to solr schema.xml (Note: upgrade instructions will need to include instructions for schema.xml):
<field name="fileRestricted" type="boolean" stored="true" indexed="false" multiValued="false"/>
<field name="canDownloadFile" type="boolean" stored="true" indexed="false" multiValued="false"/>
<field name="variableCount" type="plong" stored="true" indexed="false" multiValued="false"/>
<field name="observations" type="plong" stored="true" indexed="false" multiValued="false"/>

See https://github.com/IQSS/dataverse/issues/11027
